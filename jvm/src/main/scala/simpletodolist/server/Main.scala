package simpletodolist.server

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import org.apache.commons.daemon._

import simpletodolist.library.{Item, StorageId}

class Main extends Daemon {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher


  val storage = system.actorOf(Storage.props(List.empty[Item], StorageId.ZERO))
  val server = Server(storage)

  implicit val bind: Future[Http.ServerBinding] = Http().bindAndHandle(
    Route.handlerFlow(server.route),
    sys.props.getOrElse("listening.interface", "localhost"),
    sys.props.getOrElse("listening.port", "8080").toInt
  )

  def init(daemonContext: DaemonContext): Unit = {
    init(daemonContext.getArguments)
  }

  def init(args: Array[String]): Unit = {
    println("init")
  }

  def start(): Unit = {
    println("start")
    bind.onComplete(_ match {
      case Success(b) => println(s"Server online at ${b.localAddress.getHostString}:${b.localAddress.getPort}")
      case Failure(e) => System.err.println(s"Server didn't started: ${e.getLocalizedMessage}")
    })
  }

  def stop(): Unit = {
    println("stop")
    val end = bind.map { _.unbind() }
    Await.ready(end, Duration.Inf)
    server.sharedKillSwitch.shutdown()
    system.terminate()
  }

  def destroy(): Unit = {
    println("destroy")
  }
}

object Main {
  private lazy val server = new Main

  def main(args: Array[String]): Unit = {
    server.init(Array.empty[String])
    server.start()

    println("\n>>>>>>>>>> Press RETURN to stop... <<<<<<<<<<\n")
    StdIn.readLine() // let it run until user presses return

    server.stop()
    server.destroy()
  }
}

