package tdl

import java.nio.{file => nf}

import cats.effect.*
import cats.syntax.option.given
import fs2.io.file.Path
import io.circe.syntax.*
import org.http4s.EntityEncoder
import org.http4s.HttpRoutes
import org.http4s.ServerSentEvent
import org.http4s.StaticFile
import org.http4s.circe.*
import org.http4s.dsl.io.*

import tdl.api.*
import tdl.api.PathExtractors.*
import tdl.model.*

def routes(store: Store[IO]) = HttpRoutes
  .of[IO] {
    case GET -> Root / "list" =>
      given [F[_]]: EntityEncoder[F, List[ListInfo]] = jsonEncoderOf
      for
        lists <- store.getLists()
        resp <- Ok(lists.map(ListInfo.apply.tupled(_)))
      yield resp
    case GET -> Root / "list" / ListIdVar(id) =>
      store.get(id).flatMap {
        case Some((name, rs)) => Ok(TodoList(id, name, rs))
        case None             => NotFound(s"The $id list wasn't found.")
      }
    case req @ POST -> Root / "list" / ListIdVar(id) =>
      req
        .as[TodoListData]
        .flatMap(l => store.replace(id, l.name, l.items))
        .flatMap(_ => Ok("OK"))
    case req @ POST -> Root / "list" / ListIdVar(id) /
        "item" / RecordIdVar(rid) =>
      req
        .as[ItemUpdate]
        .flatMap(r => store.update(id, rid, r.checked))
        .flatMap(_ => Ok("OK"))
    case GET -> Root / "list" / ListIdVar(id) / "events" =>
      Ok(
        store
          .subscribe(id)
          .map(ev => ServerSentEvent(ev.asJson.noSpaces.some)),
      )
  }

def staticRoutes(path: nf.Path) = HttpRoutes.of[IO] {
  case req @ GET -> Root if path.resolve("index.html").toFile().isFile() =>
    StaticFile
      .fromPath(Path.fromNioPath(path.resolve("index.html")), Some(req))
      .getOrElseF(NotFound())
  case req @ GET -> Root / p if path.resolve(p).toFile().isFile() =>
    StaticFile
      .fromPath(Path.fromNioPath(path.resolve(p)), Some(req))
      .getOrElseF(NotFound())
}
