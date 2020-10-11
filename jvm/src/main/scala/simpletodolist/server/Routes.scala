package simpletodolist.server

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink

import simpletodolist.library.Command

trait Routes(using materializer: Materializer, ec: ExecutionContext) {
  def pipeline: Flow[Command, Command, Any]

  val messageToCommand: Flow[Message, Command, Any] =
    Flow[Message].mapAsync(1) {
      case m: TextMessage => m.textStream.runWith(Sink.fold("")(_ + _)).map(Command(_))
      case _: BinaryMessage => Future.failed(new Exception("Unexpected binary message"))
    }

  val commandToMessage: Flow[Command, Message, Any] =
    Flow[Command].map(cmd => TextMessage(cmd.toRaw))

  final val route: Route = path("") {
    handleWebSocketMessages(messageToCommand via pipeline via commandToMessage)
  } ~ get {
    complete(HttpResponse(StatusCodes.NotFound, entity = StatusCodes.NotFound.defaultMessage))
  }
}
