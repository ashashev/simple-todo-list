package tdl.model

import cats.Functor
import cats.Monad
import cats.effect.*
import cats.effect.kernel.Outcome.Succeeded
import cats.effect.std.Random
import cats.effect.std.Random.apply
import cats.instances.all.given
import cats.syntax.all.given
import doobie.given
import doobie.implicits.given
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import munit.CatsEffectSuite

import tdl.tests.munit.Ops.*
import tdl.util.NonEmptyString

class TodoStoreSuite extends CatsEffectSuite:

  private var tmpFile_ : java.nio.file.Path = _
  private var transactor_ : doobie.Transactor[IO] = _

  override def beforeAll(): Unit =
    tmpFile_ = java.nio.file.Files.createTempFile("sqlite-test-db", "")
    transactor_ = doobie.Transactor.fromDriverManager[IO](
      "org.sqlite.JDBC",
      s"jdbc:sqlite:${tmpFile_.toString()}",
    )
    tdl.db.jdbc.scheme.init.transact(transactor_).unsafeRunSync()

  override def afterAll(): Unit =
    java.nio.file.Files.delete(tmpFile_)

  def todoListCreatReadUpdateScenario[F[_]: Sync](store: TodoStore[F]) =
    val lid = ListId("list-rfv").value
    val name = NonEmptyString("ToDo List").value

    val rids = Vector(
      RecordId("zxc1").value,
      RecordId("asd2").value,
      RecordId("qwe3").value,
    )

    val vs = Vector(
      "one item",
      "another item",
      "one more item",
    ).map(NonEmptyString(_).value)

    val rs = (rids zip vs) map { case (id, v) =>
      Record(id, v, false)
    }

    for
      _ <- store.replace(lid, name, rs.toList)
      stored <- store.get(lid)
      _ <- Sync[F].delay(assertEquals(stored, Some(name -> rs.toList)))
      _ <- store.update(lid, rs(1).id, !rs(1).checked)
      updated <- store.get(lid)
      expectedUpd = rs.updated(1, rs(1).copy(checked = !rs(1).checked)).toList
      _ <- Sync[F].delay(assertEquals(updated, Some(name -> expectedUpd)))
    yield ()

  test("in memory create-read-update") {
    todoListCreatReadUpdateScenario[IO](TodoStore.memEmpty[IO])
  }

  test("jdbc create-read-update") {
    todoListCreatReadUpdateScenario[IO](tdl.db.jdbc.TodoStore(transactor_))
  }

end TodoStoreSuite
