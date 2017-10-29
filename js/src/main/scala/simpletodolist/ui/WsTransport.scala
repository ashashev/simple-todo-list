package simpletodolist.ui

import org.scalajs.dom.WebSocket
import simpletodolist.library._

trait WsTransport {
  final def connected: Boolean = connected_
  def socket: WebSocket
  def send(data: String): Unit = {
    println("send")
    if (connected)
      socket.send(data)
    else
      println("not connected!")
  }

  def onConnect(on: Boolean): Unit = ()
  def onReceived(cmd: Command): Unit

  private final var connected_ = false
  socket.onopen = { _ =>
    println("onopen")
    connected_ = true
    onConnect(connected)
  }
  socket.onclose = { _ =>
    println("onclose")
    connected_ = false
    onConnect(connected)
  }
  socket.onerror = { _ =>
    println("onerror")
  }
  socket.onmessage = { event =>
    println("onmessage")
    onReceived(Command(event.data.asInstanceOf[String]))
  }
}
