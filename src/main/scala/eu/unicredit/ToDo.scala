package eu.unicredit

import akka.actor.{Props, PoisonPill}

import org.scalajs.dom.document.{getElementById => getElem}

import scalatags.JsDom._
import scalatags.JsDom.all._

case class ToDo(hook: String) extends DomActor {
  override val domElement = Some(getElem(hook))

  val inputBox =
    input("placeholder".attr := "what to do?").render

  def newElem() =
    () => context.actorOf(Props(ToDoElem(s"ul$hook", inputBox.value)))

  def template() =
    div(
      form(cls := "form-inline", "role".attr := "form")(
        div(cls := "form-group")(
          inputBox,
          button(
            `type` := "button",
            cls := "btn btn-default",
            onclick := newElem
          )("Add")
        )
      ),
      ul(id := s"ul$hook", cls := "list-group")
    )
}

case class ToDoElem(hook: String, value: String) extends DomActor {
  override val domElement = Some(getElem(hook))

  def template() =
    li(cls := "list-group-item")(
      p(value),
      button(
        `type` := "button",
        cls := "btn btn-default btn-sm",
        onclick := {self ! PoisonPill}
      )("Remove")
    )
}
