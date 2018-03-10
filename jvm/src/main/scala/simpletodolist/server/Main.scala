package simpletodolist.server

import akka.actor.ActorSystem
import org.apache.commons.daemon._
import simpletodolist.library.Item

import scala.io.StdIn

class Main extends Daemon {


  implicit val system = ActorSystem()

  val storage = system.actorOf(Storage.props(List.empty[Item]))
  val server = Server(
    sys.props.getOrElse("listening.interface", "localhost"),
    sys.props.getOrElse("listening.port", "8080").toInt,
    storage
  )

  def init(daemonContext: DaemonContext): Unit = {
    init(daemonContext.getArguments)
  }

  def init(args: Array[String]): Unit = {
    println("init")
  }

  def start(): Unit = {
    println("start")
    server.start()
  }

  def stop(): Unit = {
    println("stop")
    server.stop()
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

