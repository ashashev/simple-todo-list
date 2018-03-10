package simpletodolist.server

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import simpletodolist.library.Command

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Server {
  def apply(interface: String, port: Int, storage: ActorRef)(implicit system: ActorSystem): Server =
    new Server(interface, port, storage, system)
}

/**
  * WebSocket server.
  *
  * @param interface
  * @param port
  * @param storage
  * @param system
  */
class Server(interface: String, port: Int, storage: ActorRef, implicit private val system: ActorSystem) {
  import system.dispatcher
  implicit private val materializer = ActorMaterializer()

  private def newClient(): Flow[Message, Message, NotUsed] = {
    val clientActor = system.actorOf(Client.props(storage))

    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(text) => Command(text)
      }.to(Sink.actorRef[Command](clientActor, PoisonPill))

    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef[Command](10, OverflowStrategy.fail)
        .mapMaterializedValue { outActor =>
          clientActor ! Client.Connected(outActor)
          NotUsed
        }.map {
        cmd => TextMessage(cmd.toRaw)
      }
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }

  private val requestHandler: HttpRequest => HttpResponse = {
    case req@HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) => upgrade.handleMessages(newClient())
        case None => HttpResponse(400, entity = "Not a valid websocket request!")
      }
    case r: HttpRequest =>
      r.discardEntityBytes() // important to drain incoming HTTP Entity stream
      HttpResponse(404, entity = "Unknown resource!")
  }

  private var binding: Option[Future[Http.ServerBinding]] = None

  /**
    * Binds socket.
    */
  def start(): Unit = binding match {
    case None =>
      val f = Http().bindAndHandleSync(requestHandler, interface = interface, port = port)
      f.onComplete(_ => println(s"Server online at http://$interface:$port/"))
      binding = Some(f)
    case Some(_) => throw new Exception("Server is already started!")
  }

  /**
    * Unbinds socket.
    */
  def stop(): Unit = binding.map { f =>
    val end = f.flatMap(_.unbind())
    Await.result(end, Duration.Inf)
  }

}


