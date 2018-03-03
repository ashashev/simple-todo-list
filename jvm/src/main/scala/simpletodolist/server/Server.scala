package simpletodolist.server

import akka.NotUsed
import akka.actor.{ActorSystem, PoisonPill}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import simpletodolist.library._


class Server {
  implicit lazy val system = ActorSystem()
  implicit lazy val materializer = ActorMaterializer()

  private val initialItems = List.empty[Item]


  lazy val storage = system.actorOf(Storage.props(initialItems), "Storage")

  def newClient(): Flow[Message, Message, NotUsed] = {
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

  private lazy val requestHandler: HttpRequest => HttpResponse = {
    case req@HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) => upgrade.handleMessages(newClient())
        case None => HttpResponse(400, entity = "Not a valid websocket request!")
      }
    case r: HttpRequest =>
      r.discardEntityBytes() // important to drain incoming HTTP Entity stream
      HttpResponse(404, entity = "Unknown resource!")
  }

  val interface = sys.props.getOrElse("listening.interface", "localhost")
  val port = sys.props.getOrElse("listening.port", "8080").toInt
  lazy val bindingFuture =
    Http().bindAndHandleSync(requestHandler, interface = interface, port = port)

}


