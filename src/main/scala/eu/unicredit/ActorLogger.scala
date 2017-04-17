package eu.unicredit

import akka.actor._
import akka.event._
import Logging._

import scala.scalajs.js.annotation.JSExport

object ActorLogger {

  case class SetTargetActor(ref: ActorRef)

  var lastLogger: Option[ActorRef] = None
}

class ActorLogger extends JSDefaultLogger() {

  ActorLogger.lastLogger = Some(self)

  private val date = new java.util.Date()
  private val dateFormat = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS")
  private val errorFormat = "[ERROR] [%s] [%s] [%s] %s%s"
  private val errorFormatWithoutCause = "[ERROR] [%s] [%s] [%s] %s"
  private val warningFormat = "[WARN] [%s] [%s] [%s] %s"
  private val infoFormat = "[INFO] [%s] [%s] [%s] %s"
  private val debugFormat = "[DEBUG] [%s] [%s] [%s] %s"

  var targetActor: Option[ActorRef] = None

  override def receive: Receive = super.receive orElse {
    case ActorLogger.SetTargetActor(ref) => targetActor = Some(ref)
  }

  override def error(event: Error): Unit = {
    val f = if (event.cause == Error.NoCause) errorFormatWithoutCause else errorFormat
    targetActor.map(_ !
    LogMsg(f.format(
      timestamp(event),
      event.thread.getName,
      event.logSource,
      event.message,
      stackTraceFor(event.cause)))
    )
  }

  override def warning(event: Warning): Unit =
    targetActor.map(_ !
    LogMsg(warningFormat.format(
      timestamp(event),
      event.thread.getName,
      event.logSource,
      event.message))
    )

  override def info(event: Info): Unit = {
    targetActor.map(_ !
    LogMsg(infoFormat.format(
      timestamp(event),
      event.thread.getName,
      event.logSource,
      event.message))
    )
  }

  override def debug(event: Debug): Unit =
    targetActor.map(_ !
    LogMsg(debugFormat.format(
      timestamp(event),
      event.thread.getName,
      event.logSource,
      event.message))
    )

  }
