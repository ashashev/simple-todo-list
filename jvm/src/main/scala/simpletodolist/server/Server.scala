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

import scala.io.StdIn
import org.apache.commons.daemon._

class Server extends Daemon {
  private implicit lazy val system = ActorSystem()
  private implicit lazy val materializer = ActorMaterializer()

  private val initialItems = List.empty[Item]


  private lazy val storage = system.actorOf(Storage.props(initialItems), "Storage")

  private def extractListId(queryString: Option[String]): Option[String] = queryString match {
    case Some(query) =>
      val id = query.split("&").filter(_.startsWith("id=")).head.drop(3)
      if (id.isEmpty) None
      else Some(id)
    case None => None
  }

  def newClient(id: String): Flow[Message, Message, NotUsed] = {
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
    case req@HttpRequest(GET, url@Uri.Path("/"), _, _, _) =>
      extractListId(url.rawQueryString) match {
        case Some(id) =>
          req.header[UpgradeToWebSocket] match {
            case Some(upgrade) => upgrade.handleMessages(newClient(id))
            case None =>
              req.discardEntityBytes()
              HttpResponse(400, entity = "Not a valid websocket request!")
          }
        case None => req.discardEntityBytes()
          HttpResponse(400, entity = "Unknown id!")
      }
    case r: HttpRequest =>
      r.discardEntityBytes() // important to drain incoming HTTP Entity stream
      HttpResponse(404, entity = "Unknown resource!")
  }

  val interface = sys.props.getOrElse("listening.interface", "localhost")
  val port = sys.props.getOrElse("listening.port", "8080").toInt
  private lazy val bindingFuture =
    Http().bindAndHandleSync(requestHandler, interface = interface, port = port)

  println(s"Server online at http://$interface:$port/")

  def init(daemonContext: DaemonContext): Unit = {
    init(daemonContext.getArguments)
  }

  def init(args: Array[String]): Unit = {
    println("init")
  }

  def start(): Unit = {
    println("start")
    system
    materializer
    storage
    bindingFuture
  }

  def stop(): Unit = {
    println("stop")
    import system.dispatcher // for the future transformations
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  def destroy(): Unit = {
    println("destroy")
  }
}

object Main {
  private lazy val server = new Server

  def main(args: Array[String]): Unit = {
    server.init(Array.empty[String])
    server.start()

    println("\n>>>>>>>>>> Press RETURN to stop... <<<<<<<<<<\n")
    StdIn.readLine() // let it run until user presses return

    server.stop()
    server.destroy()
  }
}


