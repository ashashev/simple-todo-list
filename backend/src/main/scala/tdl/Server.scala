package tdl

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.ember.server.*

import tdl.model.*

object Server extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    for
      store <- Store[IO](TodoStore.memEmpty)
      ec <- EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(routes(store))
        .build
        .use(_ => IO.never)
        .as(ExitCode.Success)
    yield ec

end Server
