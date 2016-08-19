package eu.unicredit

import scala.scalajs.js

object Fiddles {

  def urlEncode(str: String) = js.Dynamic.global.encodeURIComponent(str).asInstanceOf[String]

  val pingpong = urlEncode(
"""
import fiddle.Fiddle, Fiddle.println
import scalajs.js

// $FiddleDependency eu.unicredit %%% akkajsactorstream % 0.2.0

@js.annotation.JSExport
object ScalaFiddle {
// $FiddleStart
import akka.actor._

lazy val system = ActorSystem("pingpong")

def ppActor(matcher: String, answer: String) = Props(
new Actor {
  def receive = {
    case matcher =>
      sender ! answer
      println(s"received $matcher sending answer $answer")
    }
  }
)

val ponger = system.actorOf(ppActor("ping", "pong"))
val pinger = system.actorOf(ppActor("pong", "ping"))

import system.dispatcher
import scala.concurrent.duration._
system.scheduler.scheduleOnce(1 second)(
  pinger.!("pong")(ponger)
)

system.scheduler.scheduleOnce(2 seconds){
  pinger ! PoisonPill
  ponger ! PoisonPill
  system.terminate()
}
// $FiddleEnd
}""")

  val todo = urlEncode(
"""
import fiddle.Fiddle, Fiddle.println
import scalajs.js

// $FiddleDependency eu.unicredit %%% akkajsactorstream % 0.2.0

@js.annotation.JSExport
object ScalaFiddle {

import akka.actor._
import org.scalajs.dom.raw
import scalatags.JsDom._
import scalatags.JsDom.all._
import org.scalajs.dom.document.{getElementById => getElem}

object DomMsgs {
  case object NodeAsk
  case class Parent(node: raw.Node)
  case class Remove(node: raw.Node)
}

import DomMsgs._

trait DomActor extends Actor {

  case object Update

  val domElement: Option[raw.Node] = None

  def template: TypedTag[_ <: raw.Element]

  protected var thisNode: raw.Node = _

  def receive = domRendering

  protected def initDom(p: raw.Node): Unit = {
    thisNode = template().render
    p.appendChild(thisNode)
  }

  private def domRendering: Receive = {
    domElement match {
      case Some(de) =>
        val parent = de.parentNode
        parent.removeChild(de)
        initDom(parent)

        operative
      case _ =>
        context.parent ! NodeAsk

        domManagement orElse {
          case Parent(node) =>
            initDom(node)
            context.become(operative)
        }
    }
  }

  def domManagement: Receive =
    updateManagement orElse {
      case NodeAsk =>
        sender ! Parent(thisNode)
      case Remove(child) =>
        thisNode.removeChild(child)
    }

  def updateManagement: Receive = {
    case Update =>
      val p = thisNode.parentNode
      val oldNode = thisNode
      thisNode = template().render

      p.replaceChild(thisNode, oldNode)
  }

  def operative: Receive = domManagement

  override def postStop() = {
    context.parent ! Remove(thisNode)
  }
}

  // $FiddleStart
case class NewElem(txt: String)

case class Page() extends DomActor {
  override val domElement = Some(getElem("uiRoot"))

  val inputBox =
    input(attr("placeholder") := "what to do?",
      onkeydown := {(event: js.Dynamic) =>
        if(event.keyCode == 13) {
          event.preventDefault()
          val text = ""+event.target.value
          event.target.value = ""
          list ! NewElem(text)
        }
      }
    ).render

  val list =
    context.actorOf(Props(ToDoList()))

    def template() = div(
        h2(style := "margin: 10px;")("ToDo:"),
        inputBox
      )
}

case class ToDoList() extends DomActor {
  def template = ul()

  override def operative = domManagement orElse {
    case NewElem(txt) =>
      context.actorOf(Props(ToDoElem(txt)))
  }
}

case class ToDoElem(value: String) extends DomActor {
  def template() =
    li(
      style := "margin-top: 10px; width: 150px;",
      value,
      button(
        style :="float: right;",
        onclick := {
          () => self ! PoisonPill
        }
      )("Remove")
    )
}

val system = ActorSystem()

println(div(id:="uiRoot"))
system.actorOf(Props(Page()))
  // $FiddleEnd
}
""")

  val streams = urlEncode(
"""
import fiddle.Fiddle, Fiddle.println
import scalajs.js

@js.annotation.JSExport
object ScalaFiddle {
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.Timeout

import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.duration._

// $FiddleDependency eu.unicredit %%% akkajsactorstream % 0.2.0

// simple XORshift* random number generator (see, e.g., http://en.wikipedia.org/wiki/Xorshift)
class RandomLongValueGenerator(seed: Long = 182642182642182642L) extends Iterator[Long] {
  private[this] var state = seed

  def hasNext = true

  def next(): Long = {
    var x = state
    x ^= x << 21
    x ^= x >>> 35
    x ^= x << 4
    state = x
    (x * 0x2545f4914f6cdd1dL) - 1
  }
}

class RandomDoubleValueGenerator(seed: Long = 182642182642182642L) extends Iterator[Double] {
  private[this] val inner = new RandomLongValueGenerator(seed)

  def hasNext = true

  def next(): Double = (inner.next() & ((1L << 53) - 1)) / (1L << 53).toDouble
}

  // $FiddleStart
//Common
implicit val system = ActorSystem()
implicit val dispatcher = system.dispatcher
implicit val materializer = ActorMaterializer()

//Factorial
val factorial = Source(1 to 10).scan(1)(_ * _)

val strings =  Source(1 to 10).map(_.toString)

val throttledAndZipped = Flow[String]
  .zip(factorial).map{case (index, fact) => s"factorial(${index}) = ${fact}"}
  .throttle(1, 500 millis, 1, ThrottleMode.shaping)
  .mapAsync(10)(a => Future{s"async -> $a"})

val factorialFlow =
  throttledAndZipped.map{ea: String => println(s"factorial: ${ea}")}

//Pi
//thanks to @sirthias: https://github.com/sirthias/rs-comparison
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

val piFlow =
  Source.tick(1 millis, 100 nanos, ()).map(_ => generator.next)
    .grouped(2)
    .map { case x +: y +: Nil ⇒ Point(x, y) }
    .via(broadcastFilterMerge)
    .scan(State(0, 0)) { _ withNextSample _ }
    .conflateWithSeed(identity)(Keep.right)
    .via(onePerSecValve)
    .map(state ⇒ f"pi: After ${state.totalSamples}%,10d samples π is approximated as ${state.π}%.5f")
    .take(100000)
    .map(println)

system.scheduler.scheduleOnce(0 millis){
  factorialFlow.to(Sink.onComplete(_ =>
    piFlow.runWith(Sink.onComplete(_ ⇒ system.terminate()))
  )).runWith(strings)
}
  // $FiddleEnd
}
"""
)
}
