package simpletodolist.ui

import org.scalajs.dom.WebSocket
import org.scalajs.dom.raw.{CloseEvent, Event, MessageEvent}
import simpletodolist.library._


object WsTransport {

  trait Observer {
    def onConnect(on: Boolean): Unit = ()

    def onReceive(cmd: Command): Unit = ()
  }

  def apply(): WsTransport = new WsTransport

  def apply(url: String): WsTransport = {
    val s = WsTransport()
    s.connect(url)
    s
  }

  def apply(url: String, observer: Observer): WsTransport = {
    val s = WsTransport()
    s.addObserver(observer)
    s.connect(url)
    s
  }
}

/**
  * WebSocket wrapper. It lets connect again without recreate object.
  *
  */
final class WsTransport {

  import WsTransport._

  private var socket: Option[WebSocket] = None
  private val observers = scala.collection.mutable.Set.empty[Observer]
  private var connected_ = false

  private def onOpen(event: Event): Unit = {
    println("onopen")
    connected_ = true
    observers foreach {
      _.onConnect(connected_)
    }
  }

  private def onClose(event: CloseEvent): Unit = {
    println("onclose: " + event.reason)
    connected_ = false
    observers foreach {
      _.onConnect(connected_)
    }
  }

  private def onError(event: Event): Unit = {
    println("onerror")
  }

  private def onMessage(event: MessageEvent): Unit = {
    println("onmessage")
    val cmd = Command(event.data.asInstanceOf[String])
    observers foreach {
      _.onReceive(cmd)
    }
  }

  def addObserver(observer: Observer): Unit = {
    assert(!observers.contains(observer))
    observers += observer
  }

  def removeObserver(observer: Observer): Unit = {
    assert(observers.contains(observer))
    observers -= observer
  }

  def connected: Boolean = connected_

  def connect(url: String): Unit = {
    close()

    socket = Some(new WebSocket(url))
    socket.map { socket =>
      socket.onopen = onOpen
      socket.onclose = onClose
      socket.onerror = onError
      socket.onmessage = onMessage
    }
  }

  def close(): Unit = {
    socket.map(_.close())
    socket = None
  }

  def send(data: String): Unit = {
    println("send")
    assert(connected && socket.isDefined)
    socket.map(_.send(data))
  }

}
