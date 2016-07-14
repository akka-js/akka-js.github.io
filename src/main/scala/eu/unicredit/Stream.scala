package eu.unicredit

import akka.actor.{ActorSystem, ActorRef}
import akka.stream._
import akka.stream.scaladsl._
import akka.util.Timeout

import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.duration._

import java.util.UUID.randomUUID

import ActorLogger._

object FlowType {
  trait FlowType
  case object Factorial extends FlowType
  case object Pi extends FlowType
}

class Stream {
  import FlowType._

  var system: ActorSystem = _

  def start(ref: ActorRef, typ: FlowType) = {
    typ match {
      case Factorial => factorial(ref)
      case Pi => pi(ref)
    }
  }

  def factorial(ref: ActorRef) = {
    val systemName = s"streamfact${randomUUID}"
    system = ActorSystem(systemName, AkkaConfig.actorStreamLoggingConf)

    implicit val actorSystem = system
    implicit val dispatcher = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val factorial = Source(1 to 10).scan(1)(_ * _)

    val strings =  Source(1 to 10).map(_.toString)

    val throttledAndZipped = Flow[String]
      .zip(factorial).map{case (index, fact) => s"factorial(${index}) = ${fact}"}
      .throttle(1, 500 millis, 1, ThrottleMode.shaping)
      .mapAsync(10)(a => Future{s"async -> $a"})

    val flow =
      throttledAndZipped.
      to(Sink.foreach{
          ea: String => system.log.info(s"sink: ${ea}")
      })

    system.scheduler.scheduleOnce(100 millis){
      ActorLogger.lastLogger.map(_ ! SetTargetActor(ref))

      flow.runWith(strings)
    }
  }

  //thanks to @sirthias: https://github.com/sirthias/rs-comparison
  def pi(ref: ActorRef) = {
    val systemName = s"streampi${randomUUID}"
    system = ActorSystem(systemName, AkkaConfig.actorStreamLoggingConf)

    implicit val actorSystem = system
    implicit val dispatcher = system.dispatcher
    implicit val materializer = ActorMaterializer()

    case class Point(x: Double, y: Double) {
      def isInner: Boolean = x * x + y * y < 1.0
    }

    sealed trait Sample
    case class InnerSample(point: Point) extends Sample
    case class OuterSample(point: Point) extends Sample

    case class State(totalSamples: Long, inCircle: Long) {
      def π: Double = (inCircle.toDouble / totalSamples) * 4.0
      def withNextSample(sample: Sample) =
        State(totalSamples + 1, if (sample.isInstanceOf[InnerSample]) inCircle + 1 else inCircle)
    }

    case object Tick

    def broadcastFilterMerge =
      GraphDSL.create() { implicit b ⇒
        import GraphDSL.Implicits._

        val broadcast = b.add(Broadcast[Point](2)) // split one upstream into 2 downstreams
        val filterInner = b.add(Flow[Point].filter(_.isInner).map(InnerSample))
        val filterOuter = b.add(Flow[Point].filter(!_.isInner).map(OuterSample))
        val merge = b.add(Merge[Sample](2)) // merge 2 upstreams into one downstream

        broadcast.out(0) ~> filterInner ~> merge.in(0)
        broadcast.out(1) ~> filterOuter ~> merge.in(1)

        FlowShape(broadcast.in, merge.out)
      }

    def onePerSecValve =
      GraphDSL.create() { implicit b ⇒
        import GraphDSL.Implicits._

        val zip = b.add(ZipWith[State, Tick.type, State](Keep.left)
          .withAttributes(Attributes.inputBuffer(1, 1)))
        val dropOne = b.add(Flow[State].drop(1))

        Source.tick(Duration.Zero, 1.second, Tick) ~> zip.in1
        zip.out ~> dropOne.in

        FlowShape(zip.in0, dropOne.outlet)
      }

    val generator = new RandomDoubleValueGenerator()

    val flow =
      Source.tick(1 millis, 10 nano, ()).map(_ => generator.next)
        .grouped(2)
        .map { case x +: y +: Nil ⇒ Point(x, y) }
        .via(broadcastFilterMerge)
        .scan(State(0, 0)) { _ withNextSample _ }
        .conflateWithSeed(identity)(Keep.right)
        .via(onePerSecValve)
        .map(state ⇒ f"After ${state.totalSamples}%,10d samples π is approximated as ${state.π}%.5f")
        .take(100000)
        .map(system.log.info)

      system.scheduler.scheduleOnce(100 millis){
        ActorLogger.lastLogger.map(_ ! SetTargetActor(ref))

        flow.runWith(Sink.onComplete(_ ⇒ system.terminate()))
      }
  }

  def stop() = {
    Try{ system.terminate() }
  }

}
