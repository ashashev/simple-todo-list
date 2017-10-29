package simpletodolist.ui

import scala.language.implicitConversions
import scala.language.reflectiveCalls

import org.scalajs.dom
import dom.document
import dom.html

class Checkbox(val text: String, initChecked: Boolean, val id: String) {
  private def createElem[T](name: String) = document.createElement(name).asInstanceOf[T]
  private val wrapper = createElem[html.Div]("div")
  wrapper.id = id
  private val label = createElem[html.Span]("span")
  label.appendChild(document.createTextNode(text))
  private val checkbox = createElem[html.Input]("input")
  checkbox.`type` = "checkbox"
  wrapper.classList.add("checkbox")

  type Observer = AnyRef {def onChanged(checkbox: Checkbox): Unit}

  private var observers: Set[Observer] = Set()

  private def onChange(): Unit = {
    observers foreach (_.onChanged(this))
  }

  def checked: Boolean = checkbox.checked

  def addObserver(observer: Observer): Unit = observers += observer
  def removeObserver(observer: Observer): Unit = observers -= observer


  checkbox.checked = initChecked

  wrapper.appendChild(checkbox)
  wrapper.appendChild(label)

  checkbox.onchange = { _ =>
    onChange()
  }
  label.onclick = { _ =>
    checkbox.checked = !checkbox.checked
    onChange()
  }
}

object Checkbox {
  def apply(text: String): Checkbox = apply(text, false, "")

  def apply(text: String, checked: Boolean, id: String): Checkbox = {
    new Checkbox(text, checked, id)
  }

  implicit def CheckBoxToDomNode(checkbox: Checkbox): dom.Node = checkbox.wrapper
}