package simpletodolist.server

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{KillSwitches, OverflowStrategy}
import akka.http.scaladsl.server.Directives._
import simpletodolist.library.Command

object Server {
  def apply(storage: ActorRef)(implicit system: ActorSystem): Server =
    new Server(storage, system)
}

/**
  * The server handles incoming http-connections.
  * It regards a WebSocket connections as a command connections and routes ones to the storage.
  *
  * @param storage
  * @param system
  */
class Server(storage: ActorRef, implicit private val system: ActorSystem) {

  val sharedKillSwitch = KillSwitches.shared("my-kill-switch")

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
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages).via(sharedKillSwitch.flow)
  }

  val route = {
    get {
      handleWebSocketMessages(newClient())
    } ~
    get {
      complete(HttpResponse(StatusCodes.NotFound, entity = StatusCodes.NotFound.defaultMessage))
    }
  }
}


