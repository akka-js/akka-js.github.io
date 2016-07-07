package eu.unicredit

import akka.actor.{Props, PoisonPill}

import scala.scalajs.js

import org.scalajs.dom.document.{getElementById => getElem}
import org.scalajs.dom.window.alert

import scalatags.JsDom._
import scalatags.JsDom.all._

case class ToDo(hook: String) extends DomActor {
  override val domElement = Some(getElem(hook))

  val inputBox =
    input("placeholder".attr := "what to do?",
          `type` := "text",
          cls := "form-control",
          onkeydown := {(event: js.Dynamic) =>
            if(event.keyCode == 13) {
              event.preventDefault()
              addElem()
            }
          }).render

  val listActor = context.actorOf(Props(ToDoList(10)))

  val addElem: () => Unit = () => listActor ! inputBox.value

  def template() =
    div(
      div(cls := "input-group")(
        inputBox,
        span(cls := "input-group-btn")(
          button(
            `type` := "button",
            cls := "btn btn-default",
            onclick := addElem
          )("Add")
        )
      ),
      div(cls := "alert", style := "padding:0px")
    )
}

case class ToDoList(maxLi: Int) extends DomActor {

  def template() = div()

  override def operative = domManagement orElse {
    case value: String =>
      if (context.children.size >= maxLi)
         alert("list limit reached.")
      else
        context.actorOf(Props(ToDoElem(value)))
  }
}

case class ToDoElem(value: String) extends DomActor {

  def template() =
    div(cls := "row")(
      div(cls := "col-md-1"),
      div(cls := "col-md-8")(
        p(
          span(cls := "glyphicon glyphicon-chevron-right"),
          s" $value"
        )
      ),
      div(cls := "col-md-3")(
        span(cls := "glyphicon glyphicon-remove",
             style := "cursor:pointer",
             onclick := {() => self ! PoisonPill}
        )
      )
    )
}
