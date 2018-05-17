package simpletodolist.ui

import org.scalajs.dom._
import simpletodolist.library._

trait Control {
  def init()

  protected def getElementById[T](id: String) = {
    document.getElementById(id).asInstanceOf[T]
  }

  protected def setState(state: States.State): Unit = {
    setState(document.getElementById("status-led"))(state)
  }

  protected def setState(el: Element)(state: States.State): Unit = {
    if (el != null) {
      States.allStates foreach { s =>
        if (s == state) {
          if (!el.classList.contains(s.cssClass))
            el.classList.add(state.cssClass)
        } else {
          if (el.classList.contains(s.cssClass))
            el.classList.remove(s.cssClass)
        }
      }
    }
  }
}

object States {

  trait State {
    def cssClass: String
  }

  private val registred = collection.mutable.Set.empty[State]

  private case class SomeState(cssClass: String) extends State {
    registred += this
  }

  def allStates: Set[State] = registred.toSet

  val error: State = SomeState("led-red")
  val waiting: State = SomeState("led-yellow")
  val connected: State = SomeState("led-green")

}

object Control {
  def apply(config: Config): Control = config.mode match {
    case Mode.View => new Viewer(config)
    case Mode.Edit => new Editor(config)
    case _ => new Control {
      override def init(): Unit = println("Unknown mode or mode isn't set!")
    }
  }
}

private class Viewer(config: Config) extends Control with WsTransport.Observer {
  private var checkboxes = List.empty[Checkbox]
  private var items = List.empty[Item]
  private val itemsArea = getElementById[html.Div]("items")
  private val ws = WsTransport(config.wsUrl, this)


  override def onConnect(on: Boolean): Unit = {
    println(s"connection to ${config.wsUrl} is $on")
    if (on) {
      requestAll()
    } else {
      setState(States.error)
      items = List.empty[Item]
      recreateItems()
    }
  }

  override def onReceive(cmd: Command): Unit = {
    println(s"receive")
    setState(States.connected)
    cmd match {
      case Replace(data) =>
        items = data
        recreateItems()
      case Update(newItem) =>
        val ind = items.indexWhere(newItem.id == _.id)
        if (ind == -1) requestAll()
        else {
          items = items.updated(ind, newItem)
          recreateItems()
        }
      case _ => ()
    }
  }

  def requestAll() = {
    setState(States.waiting)
    ws.send(Get.toRaw)
  }

  def onChanged(checkbox: Checkbox): Unit = {
    val id = Item.idFromString(checkbox.id)
    val ind = items.indexWhere(id == _.id)
    val newItem = items(ind).copy(checked = checkbox.checked)
    items = items.updated(ind, newItem)
    ws.send(Update(newItem).toRaw)
  }

  private def recreateItems(): Unit = {
    checkboxes foreach { node =>
      node.removeObserver(this)
      itemsArea.removeChild(node)
    }

    checkboxes = for {
      item <- items
    } yield {
      Checkbox(item.text, item.checked, item.id.toString)
    }

    checkboxes foreach { node =>
      itemsArea.appendChild(node)
      node.addObserver(this)
    }
  }

  def onFocus(focusEvent: FocusEvent): Unit = {
    println("onFocus")
    if (!ws.connected)
      ws.connect(config.wsUrl)
  }

  def onBlur(focusEvent: FocusEvent): Unit = {
    println("onBlur")
  }

  override def init(): Unit = {
    println("Viewer")
    setState(States.error)
    window.onfocus = onFocus
    window.onblur = onBlur
    ()
  }
}

private class Editor(config: Config) extends Control {
  private val todolist = getElementById[html.TextArea]("todolist")
  private var worker: Option[Worker] = _
  private val btnReload = getElementById[html.Button]("btnReload")
  private val btnUpdate = getElementById[html.Button]("btnUpdate")

  private abstract class Worker(val stateEl: Element) extends WsTransport.Observer {
    private var canChangeState = true
    private val ws = WsTransport(config.wsUrl, this)

    setState(stateEl)(States.waiting)

    private def isAbleChangeState = worker match {
      case None => false
      case Some(w) => (w == this) && canChangeState
    }

    def connectAction(): Unit

    def finished(): Unit = ()

    def replaceAction(data: List[Item]): Unit =
      todolist.value = data.map(i => (if (i.checked) "+" else "") + i.text).mkString("\n")

    override def onConnect(on: Boolean): Unit = {
      if (on) connectAction()
      else if (isAbleChangeState) setState(stateEl)(States.error)
    }

    override def onReceive(cmd: Command): Unit = cmd match {
      case Replace(data) => if (isAbleChangeState) {
        setState(stateEl)(States.connected)
        replaceAction(data)
        canChangeState = false
        ws.removeObserver(this)
        ws.close()
        worker = None
        finished()
      }
      case _ => ()
    }

    def send = ws.send(_)
  }

  private class Getter extends Worker(document.getElementById("status-led")) {
    override def connectAction(): Unit = send(Get.toRaw)
  }

  private class Replacer extends Worker(document.getElementById("status-led")) {
    override def connectAction(): Unit =
      send(Replace(Item.makeList(todolist.value)).toRaw)

    override def finished(): Unit = {
      document.location.replace("index.html")
    }
  }

  override def init(): Unit = {
    println("Editor")
    btnReload.onclick = { _ =>
      worker = Some(new Getter)
    }
    btnUpdate.onclick = { _ =>
      worker = Some(new Replacer)
    }
    worker = Some(new Getter)
  }
}

