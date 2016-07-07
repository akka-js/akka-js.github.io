package eu.unicredit

import akka.actor._
import scala.util.Try
import scala.concurrent.duration._

import java.util.UUID.randomUUID

import ActorLogger._

class PingPong {
  var system: ActorSystem = _

  def start(ref: ActorRef) = {
    val systemName = s"pingpong${randomUUID}"
    system = ActorSystem(systemName, AkkaConfig.actorLoggingConf)

    def ppActor(matcher: String, answer: String) = Props(
      new Actor with ActorLogging {
          log.warning(s"Starting $matcher -> $answer")

          def receive = {
            case matcher =>
              sender ! answer
              log.info(s"received $matcher sending answer $answer")
          }
        }
      )

    val ponger = system.actorOf(ppActor("ping", "pong"))
    val pinger = system.actorOf(ppActor("pong", "ping"))

    implicit val dispatcher = system.dispatcher
    system.scheduler.scheduleOnce(100 millis){
      ActorLogger.lastLogger.map(_ ! SetTargetActor(ref))

      pinger.!("pong")(ponger)
    }
  }

  def stop() = {
    Try{ system.terminate() }
  }

}
