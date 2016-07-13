package eu.unicredit

import akka.actor.{Props, PoisonPill}

import scala.scalajs.js

import org.scalajs.dom.document.{getElementById => getElem}
import org.scalajs.dom.window.alert
import org.scalajs.dom.raw._

import scalatags.JsDom._
import scalatags.JsDom.all._

case class Chat(hook: String) extends DomActor {
  override val domElement = Some(getElem(hook))

  val urlBox =
    input("placeholder".attr := "enter url here",
          `type` := "text",
          cls := "form-control",
          onkeydown := {(event: js.Dynamic) =>
            if(event.keyCode == 13) {
              event.preventDefault()
              addChat()
            }
          }).render

  val boxesList = context.actorOf(Props(ChatList(5)))

  val addChat: () => Unit = () => boxesList ! urlBox.value

  def template() =
    div(
      div(cls := "input-group")(
        urlBox,
        span(cls := "input-group-btn")(
          button(
            `type` := "button",
            cls := "btn btn-default",
            onclick := addChat
          )("Connect")
        )
      ),
      div(cls := "alert", style := "padding:0px")
    )
}

case class ChatList(maxLi: Int) extends DomActor {

  def template() = div()

  override def operative = domManagement orElse {
    case value: String =>
      if (context.children.size >= maxLi)
         alert("list limit reached.")
      else
        context.actorOf(Props(ChatBox(value.replace("https://","").replace("http://",""))))
  }
}

case class ChatBox(wsUrl: String) extends DomActorWithParams[List[String]] {

  case class NewMsg(txt: String)

  val ws = new WebSocket(s"ws://$wsUrl")
  ws.onmessage = { (event: MessageEvent) => self ! NewMsg(event.data.toString)}

  val initValue = List()

  val msgBox =
    input("placeholder".attr := "enter message",
      `type` := "text",
      cls := "form-control",
      onkeydown := {(event: js.Dynamic) =>
        if(event.keyCode == 13) {
          event.preventDefault()
          sendMsg()
        }
      }
    ).render

  val sendMsg: () => Unit = () => ws.send(msgBox.value)

  def template(txt: List[String]) =
    div(cls := "row")(
      div(cls := "col-md-1"),
      div(cls := "col-md-10")(
        b(s"connected to: $wsUrl"),
        div(cls := "input-group")(
          msgBox,
          span(cls := "input-group-btn")(
            button(
              `type` := "button",
              cls := "btn btn-default",
              onclick := sendMsg
            )("Send")
          )
        ),
        ul()(
          for (t <- txt) yield li()(t)
        ),
        div(cls := "input-group")(
          span(cls := "input-group-btn")(
            button(
              `type` := "button",
              cls := "btn btn-default",
              onclick := {() => self ! PoisonPill})("Close")
          )
        ),
        hr()
      ),
      div(cls := "col-md-1")
    )

  override def operative = withText(initValue)

  def withText(last: List[String]): Receive = domManagement orElse {
    case NewMsg(txt) =>
      val newTxt = (last :+ txt).takeRight(5)
      self ! UpdateValue(newTxt)
      context.become(withText(newTxt))
  }
}
