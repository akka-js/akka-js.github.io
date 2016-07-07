package eu.unicredit

import akka.actor.{Props, PoisonPill}

import scala.scalajs.js

import org.scalajs.dom.document.{getElementById => getElem}

import scalatags.JsDom._
import scalatags.JsDom.all._

case class ToDo(hook: String) extends DomActor {
  override val domElement = Some(getElem(hook))

  val inputBox =
    input("placeholder".attr := "what to do?",
          onkeydown := {(event: js.Dynamic) =>
            if(event.keyCode == 13) {
              event.preventDefault()
              addElem()
            }
          }).render

  val listActor = context.actorOf(Props(ToDoList()))

  val addElem: () => Unit = () => listActor ! inputBox.value

  def template() =
    div(
      form(cls := "form-inline",
          "role".attr := "form",
          style := "margin:0 auto;width:50%")(
        div(cls := "form-group")(
          inputBox,
          button(
            `type` := "button",
            cls := "btn btn-default btn-sm",
            onclick := addElem
          )("Add")
        )
      ),
      div(cls := "alert", style := "padding:0px")
    )
}

case class ToDoList() extends DomActor {

  def template() = ul(cls := "container")

  override def operative = domManagement orElse {
    case value: String =>
      context.actorOf(Props(ToDoElem(value)))
  }
}

case class ToDoElem(value: String) extends DomActor {

  def template() =
    li(style := "margin:0 auto;width:85%")(
      form(cls := "form-inline", "role".attr := "form")(
        div(cls := "form-group")(
            label(style := "margin-right:50px")(value),
            a(onclick := {() => self ! PoisonPill})("Remove")
        )
      )
    )
}
