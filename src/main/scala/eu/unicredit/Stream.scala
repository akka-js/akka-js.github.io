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

class Stream {
  var system: ActorSystem = _

  def start(ref: ActorRef) = {
    val systemName = s"stream${randomUUID}"
    system = ActorSystem(systemName, AkkaConfig.config/*actorLoggingConf*/)

    implicit val actorSystem = system
    implicit val dispatcher = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val factorial = Source(1 to 10).scan(1)(_ * _)

    val strings =  Source(1 to 10).map(_.toString)

    val throttledAndZipped = Flow[String]
      .zip(factorial).map{case (index, fact) => s"factorial(${index}) = ${fact}"}
      .throttle(1, 1 second, 1, ThrottleMode.shaping)
      .mapAsync(10)(a =>
        Future{s"async -> $a"}
      )

    val flow =
      throttledAndZipped.
        to(Sink.foreach{
          ea: String => system.log.info(s"sink: ${ea}")
        })

    system.scheduler.scheduleOnce(100 millis){
      //ActorLogger.lastLogger.map(_ ! SetTargetActor(ref))

      flow.runWith(strings)
    }
  }

  def stop() = {
    Try{ system.terminate() }
  }

}
