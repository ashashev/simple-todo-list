package tdl

import scala.concurrent.duration._

import cats.effect.*
import cats.effect.kernel.Outcome.Succeeded
import cats.implicits.given
import cats.instances.all.given
import cats.syntax.all.given
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import io.circe.Json
import io.circe.parser
import munit.CatsEffectSuite
import org.http4s.*
import org.http4s.MediaType.*
import org.http4s.Status.*
import org.http4s.circe.given
import org.http4s.client.Client
import org.http4s.headers.*
import org.http4s.implicits.uri
import scodec.bits.ByteVector

import tdl.api.*
import tdl.model.*
import tdl.tests.munit.Ops.*
import tdl.util.*

class RoutesSuite extends CatsEffectSuite:

  import RoutesSuite.*

  test("Get all lists when DB is empty") {
    val expected = parser.parse("[]").value
    for
      store <- Store[IO](TodoStore.memEmpty)
      cl = client(store)
      req = Request(method = Method.GET, uri = uri"/list")
      _ <- assertIO(cl.expect[Json](req), expected)
    yield ()
  }

  test("Get all lists") {
    val lid1 = ListId("list-1").value
    val name1 = NonEmptyString("ToDo List #1").value

    val lid2 = ListId("list-2").value
    val name2 = NonEmptyString("ToDo List #2").value
    val records2 =
      Record(
        RecordId("item-1").value,
        NonEmptyString("one item").value,
        false,
      ) :: Nil

    val expected = parser
      .parse("""[
        |  {"lid": "list-1", "name": "ToDo List #1"},
        |  {"lid": "list-2", "name": "ToDo List #2"}
        |]""".stripMargin)
      .value

    for
      store <- Store[IO](TodoStore.memEmpty)
      _ <- store.replace(lid1, name1, Nil)
      _ <- store.replace(lid2, name2, records2)
      cl = client(store)
      req = Request(method = Method.GET, uri = uri"/list")
      _ <- assertIO(cl.expect[Json](req), expected)
    yield ()
  }

  test("Get unexisted todo list") {
    for
      store <- Store[IO](TodoStore.memEmpty)
      cl = client(store)
      listId = "some-id"
      req = Request(method = Method.GET, uri = uri"/list" / listId)
      got <- cl.run(req).use {
        case Response(NotFound, _, _, body, _) =>
          body.compile.toVector.map(v => String(v.toArray))
        case resp => IO(fail(s"unexpected response: $resp"))
      }
      _ <- IO(assertEquals(got, s"The $listId list wasn't found."))
    yield ()
  }

  test("Get existed todo list") {
    val listId = ListId("list-1").value
    val name = NonEmptyString("ToDo List").value

    val r1 = Record(
      RecordId("qwe").value,
      NonEmptyString("item 1").value,
      false,
    )

    val r2 = Record(
      RecordId("asd").value,
      NonEmptyString("item 2").value,
      true,
    )

    val expected = parser
      .parse(
        """{
        |  "lid": "list-1",
        |  "name": "ToDo List",
        |  "items": [
        |    {
        |      "id": "qwe",
        |      "value": "item 1",
        |      "checked": false
        |    },
        |    {
        |      "id": "asd",
        |      "value": "item 2",
        |      "checked": true
        |    }
        |  ]
        |}""".stripMargin,
      )
      .value

    for
      memStore <- IO(TodoStore.memEmpty[IO])
      _ <- memStore.replace(listId, name, r1 :: r2 :: Nil)
      store <- Store[IO](memStore)
      cl = client(store)
      req = Request(method = Method.GET, uri = uri"/list" / listId.toRaw)
      got <- cl.expect[Json](req)
      _ <- IO(assertEquals(got, expected))
    yield ()
  }

  test("Subscribe to list changes") {
    val listId = ListId("list-1").value
    val name = NonEmptyString("ToDo List").value

    val expectingEvents =
      List(
        """{
        |  "lid": "list-1",
        |  "name": "ToDo List",
        |  "items": []
        |}""".stripMargin,
        """{
        |  "lid": "list-1",
        |  "name": "ToDo List",
        |  "items": [
        |    {
        |      "id": "qwe",
        |      "value": "item 1",
        |      "checked": false
        |    },
        |    {
        |      "id": "asd",
        |      "value": "item 2",
        |      "checked": false
        |    }
        |  ]
        |}""".stripMargin,
        """{
        |  "lid": "list-1",
        |  "rid": "asd",
        |  "checked": true
        |}""".stripMargin,
      ).traverse(parser.parse).value

    val expectingFinalList = parser
      .parse(
        """{
        |  "lid": "list-1",
        |  "name": "ToDo List",
        |  "items": [
        |    {
        |      "id": "qwe",
        |      "value": "item 1",
        |      "checked": false
        |    },
        |    {
        |      "id": "asd",
        |      "value": "item 2",
        |      "checked": true
        |    }
        |  ]
        |}""".stripMargin,
      )
      .value

    for
      memStore <- IO(TodoStore.memEmpty[IO])
      // Put to the store list without items
      _ <- memStore.replace(listId, name, Nil)
      store <- Store[IO](memStore)
      cl = client(store)
      helper = ClientHelper(cl)
      intSig <- SignallingRef[IO].of(false)
      // got the list's changes
      got <- helper.subscribe(intSig, listId).use { join =>
        // fill the list
        helper.sendList(
          listId,
          """{
            |  "name": "ToDo List",
            |  "items": [
            |    {
            |      "id": "qwe",
            |      "value": "item 1",
            |      "checked": false
            |    },
            |    {
            |      "id": "asd",
            |      "value": "item 2",
            |      "checked": false
            |    }
            |  ]
            |}""".stripMargin,
        ) >>
          // update item 'asd'
          helper.updateItem(
            listId,
            RecordId("asd").value,
            """{
              |  "checked": true
              |}""".stripMargin,
          ) >>
          IO.sleep(100.millis) >>
          // close events stream
          intSig.set(true) >>
          // get final list
          {
            val req =
              Request(method = Method.GET, uri = uri"/list" / listId.toRaw)
            assertIO(cl.expect[Json](req), expectingFinalList)
          } >>
          join.value
      }
      // The test sends the new state immediately after subscribing of change
      // events. As a result, the empty state of the list could be missed, so
      // here if the size of the got less than the expected we prepend the got
      // with the head of the expected.
      fixedGot =
        if got.size < expectingEvents.size
        then expectingEvents.head :: got
        else got
      _ <- IO(assertEquals(fixedGot, expectingEvents))
    yield ()
  }

  test("Multiple subscribers to list changes") {
    val listId1 = ListId("list-1").value
    val listId2 = ListId("list-2").value
    val name1 = NonEmptyString("ToDo List #1").value
    val name2 = NonEmptyString("ToDo List #2").value

    val expectingEvents1 =
      List(
        """{
        |  "lid": "list-1",
        |  "name": "ToDo List #1",
        |  "items": [
        |    {
        |      "id": "qwe",
        |      "value": "item 1",
        |      "checked": false
        |    },
        |    {
        |      "id": "asd",
        |      "value": "item 2",
        |      "checked": false
        |    }
        |  ]
        |}""".stripMargin,
        """{
        |  "lid": "list-1",
        |  "rid": "asd",
        |  "checked": true
        |}""".stripMargin,
      ).traverse(parser.parse).value

    val expectingEvents2 =
      List(
        """{
        |  "lid": "list-2",
        |  "name": "ToDo List #2",
        |  "items": [
        |    {
        |      "id": "ert",
        |      "value": "item 1",
        |      "checked": true
        |    },
        |    {
        |      "id": "cvb",
        |      "value": "item 2",
        |      "checked": false
        |    }
        |  ]
        |}""".stripMargin,
        """{
        |  "lid": "list-2",
        |  "rid": "ert",
        |  "checked": false
        |}""".stripMargin,
      ).traverse(parser.parse).value

    for
      memStore <- IO(TodoStore.memEmpty[IO])
      store <- Store[IO](memStore)
      cl = client(store)
      intSig <- SignallingRef[IO].of(false)
      helper = ClientHelper(cl)
      _ <- (
        // Initialize subscribers, two for the list-1 and one for the list-2
        for
          j1 <- helper.subscribe(intSig, listId1)
          j2 <- helper.subscribe(intSig, listId1)
          j3 <- helper.subscribe(intSig, listId2)
        yield (j1, j2, j3)
      ).use { case (join1, join2, join3) =>
        // Wait a bit for subscribers have started consuming events
        IO.sleep(100.millis) >>
          // fill the list-1
          helper.sendList(
            listId1,
            """{
              |  "name": "ToDo List #1",
              |  "items": [
              |    {
              |      "id": "qwe",
              |      "value": "item 1",
              |      "checked": false
              |    },
              |    {
              |      "id": "asd",
              |      "value": "item 2",
              |      "checked": false
              |    }
              |  ]
              |}""".stripMargin,
          ) >>
          // fill the list-2
          helper.sendList(
            listId2,
            """{
              |  "name": "ToDo List #2",
              |  "items": [
              |    {
              |      "id": "ert",
              |      "value": "item 1",
              |      "checked": true
              |    },
              |    {
              |      "id": "cvb",
              |      "value": "item 2",
              |      "checked": false
              |    }
              |  ]
              |}""".stripMargin,
          ) >>
          // update items
          helper.updateItem(
            listId1,
            RecordId("asd").value,
            """{
              |  "checked": true
              |}""".stripMargin,
          ) >>
          helper.updateItem(
            listId2,
            RecordId("ert").value,
            """{
              |  "checked": false
              |}""".stripMargin,
          ) >>
          // Give the consumers a chance to get all events
          IO.sleep(100.millis) >>
          // and close them
          intSig.set(true) >>
          // check results
          assertIO(join1.value, expectingEvents1) >>
          assertIO(join2.value, expectingEvents1) >>
          assertIO(join3.value, expectingEvents2)
      }
    yield ()
  }

end RoutesSuite

object RoutesSuite:

  def client(store: Store[IO]): Client[IO] = Client.fromHttpApp(routes(store))

  def sseParser[F[_]]: fs2.Pipe[F, Byte, Json] =
    _.through(ServerSentEvent.decoder)
      .map(_.data)
      .map(_.flatMap(s => parser.parse(s).toOption))
      .flatMap(fs2.Stream.fromOption(_))

  def sseFiber(haltWhenTrue: Signal[IO, Boolean])(
      body: fs2.Stream[IO, Byte],
  ): ResourceIO[IO[OutcomeIO[List[Json]]]] =
    body
      .through(sseParser)
      .interruptWhen(haltWhenTrue)
      .compile
      .toList
      .background

  def toEntity(s: String): Entity[IO] =
    Entity.strict(ByteVector(s.getBytes("utf8")))

  class ClientHelper(cl: Client[IO]) extends munit.Assertions:
    def subscribe(
        intSig: SignallingRef[IO, Boolean],
        listId: ListId,
    )(using loc: munit.Location): ResourceIO[IO[OutcomeIO[List[Json]]]] =
      val req = Request(
        method = Method.GET,
        uri = uri"/list" / listId.toRaw / "events",
        headers = Headers(Accept(`text/event-stream`)),
      )
      cl.run(req)
        .evalMap {
          case Response(Ok, _, _, body, _) =>
            IO(sseFiber(intSig)(body))
          case resp =>
            IO(fail(s"unexpected response: $resp"))
        }
        .flatten

    def sendList(id: ListId, body: String)(using
        loc: munit.Location,
    ): IO[Unit] =
      cl.expect[String](
        Request(
          method = Method.POST,
          uri = uri"/list" / id.toRaw,
          entity = toEntity(body),
        ),
      ).handleErrorWith(err => IO(fail(s"sendList failed: $err")))
        .as(())

    def updateItem(id: ListId, rid: RecordId, body: String)(using
        loc: munit.Location,
    ): IO[Unit] =
      cl.expect[String](
        Request(
          method = Method.POST,
          uri = uri"/list" / id.toRaw / "item" / rid.toRaw,
          entity = toEntity(body),
        ),
      ).handleErrorWith(err => IO(fail(s"updateItem failed: $err")))
        .as(())
  end ClientHelper

end RoutesSuite
