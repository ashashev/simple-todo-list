package simpletodolist.server

import scala.language.implicitConversions
import scala.concurrent.ExecutionContext

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._

object Main {
  def main(args: Array[String]): Unit = {

    given system as ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "my-system")
    given ExecutionContext = system.executionContext

    val route =
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

    println(">>>>>>>>> Press Enter to stop... <<<<<<<<")
    scala.io.StdIn.readLine()

    bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
