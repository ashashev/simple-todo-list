package tdl

import java.nio.file.Paths

import cats.effect.*
import cats.syntax.all.given
import com.comcast.ip4s.*
import org.http4s.ember.server.*
import org.http4s.server.middleware.CORS
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jFactory

//import tdl.model.*
import tdl.db.jdbc

object Server extends IOApp:

  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  given logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  def run(args: List[String]): IO[ExitCode] =
    for
      // store <- Store[IO](TodoStore.memEmpty)
      rs <- jdbc.TodoStore.sqlite[IO](Paths.get("simple-todo-list.db"))
      store <- Store[IO](rs)
      service = (routes(store) <+> staticRoutes(
        Paths.get("..", "frontend"),
      )).orNotFound
      serviceWithCors = CORS.policy.withAllowOriginAll(service)
      ec <- EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(serviceWithCors)
        .build
        .use(_ => IO.never)
        .as(ExitCode.Success)
    yield ec

end Server
