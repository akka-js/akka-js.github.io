package eu.unicredit

import akka.actor._
import scala.util.Try
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class PingPong {
  var system: ActorSystem = _

  def start(print: (String) => Unit) = {
    system = ActorSystem("pingpong", AkkaConfig.config)

    def ppActor(matcher: String, answer: String) = Props(
        new Actor {
          def receive = {
            case matcher =>
              sender ! answer
              print(s"received $matcher sending answer $answer")
          }
        }
      )

    val ponger = system.actorOf(ppActor("ping", "pong"))
    val pinger = system.actorOf(ppActor("pong", "ping"))

    system.scheduler.scheduleOnce(100 millis)(
      pinger.!("pong")(ponger)
    )
  }

  def stop() = {
    Try{ system.terminate() }
  }

}
