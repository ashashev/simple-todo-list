package simpletodolist.server

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.pattern._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{KillSwitches, OverflowStrategy}
import akka.util.Timeout

import simpletodolist.library.{Command, StorageId}

object Server {
  def apply(storage: ActorRef)(implicit system: ActorSystem): Server =
    new Server(storage, system)
}

/**
  * The server handles incoming http-connections.
  * It regards a WebSocket connections as a command connections and routes ones to the storage.
  *
  * @param sm is ActorRef on the StorageManager
  * @param system
  */
class Server(sm: ActorRef, implicit private val system: ActorSystem) {

  import StorageManager._

  val sharedKillSwitch = KillSwitches.shared("my-kill-switch")

  private def newClient(storage: ActorRef): Flow[Message, Message, NotUsed] = {
    val clientActor = system.actorOf(Client.props(storage))

    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].flatMapConcat {
        case TextMessage.Strict(text) => Source(Command(text) :: Nil)
        case TextMessage.Streamed(in) => in.reduce(_ + _).map(Command(_))
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

  private implicit val timeout = Timeout(5 seconds)

  val route = {
    get {
      parameters('id) { sid =>
        val id = StorageId.fromString(sid)
        val getStorage = (sm ? GetStorage(id)).mapTo[Option[ActorRef]]

        onComplete(getStorage) {
          case Success(Some(s)) =>
            handleWebSocketMessages(newClient(s))
          case Success(None) =>
            System.err.println(s"Could not find storage with the '${sid}' id.")
            complete((StatusCodes.BadRequest, s"Could not find storage with the '${sid}' id."))
          case Failure(ex) =>
            System.err.println(s"Can get storage: ${ex.getMessage}")
            complete((StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}"))
        }
      }
    } ~
    get {
      complete(HttpResponse(StatusCodes.NotFound, entity = StatusCodes.NotFound.defaultMessage))
    }
  }
}


