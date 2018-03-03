package simpletodolist.server

import org.apache.commons.daemon._
import scala.io.StdIn

class Main extends Server with Daemon {

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

