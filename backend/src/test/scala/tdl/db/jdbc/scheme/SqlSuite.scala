package tdl.db.jdbc.scheme

import cats.effect.*
import cats.effect.kernel.Outcome.Succeeded
import cats.effect.std.Random
import cats.effect.std.Random.apply
import cats.instances.all.given
import cats.syntax.all.given
import doobie.ConnectionIO
import doobie.Transactor
import doobie.given
import doobie.implicits.given
import doobie.munit.IOChecker
import doobie.util.update.Update
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import munit.CatsEffectSuite

import tdl.model.ListId
import tdl.model.RecordId
import tdl.tests.munit.Ops.*
import tdl.util.NonEmptyString

class SqlSuite extends CatsEffectSuite with IOChecker:
  import SqlSuite._

  private var tmpFile_ : java.nio.file.Path = _
  private var transactor_ : Transactor[IO] = _

  override def beforeAll(): Unit =
    tmpFile_ = java.nio.file.Files.createTempFile("sqlite-test-db", "")
    transactor_ = Transactor.fromDriverManager[IO](
      "org.sqlite.JDBC",
      s"jdbc:sqlite:${tmpFile_.toString()}",
    )
    init.transact(transactor_).unsafeRunSync()

  override def afterAll(): Unit =
    java.nio.file.Files.delete(tmpFile_)

  def transactor: Transactor[IO] = transactor_

  test("insertList") {
    check(
      insertList.toUpdate0(
        ListId("l1").value -> NonEmptyString("list 1").value,
      ),
    )
  }

  test("insertItem".fail) {
    // sqlite doesn't distinguish input types, all are string
    check(insertItem.toUpdate0(makeItem("l1", "i1", 1, "item 1", false)))
  }

  test("updateItem".fail) {
    // sqlite doesn't distinguish input types, all are string
    check(
      updateItem(ListId("l2").value, RecordId("i3").value, true),
    )
  }

  test("deleteItems") {
    check(deleteItems.toUpdate0(ListId("l5").value))
  }

  test("deleteList") {
    check(deleteList.toUpdate0(ListId("l5").value))
  }

  test("getLists") {
    check(getLists)
  }

  test("getListItems") {
    check(getListItems(ListId("l6").value))
  }

end SqlSuite

object SqlSuite:
  def makeItem(
      lid: String,
      id: String,
      order: Int,
      value: String,
      checked: Boolean,
  ): Item =
    Item(
      ListId(lid).value,
      RecordId(id).value,
      order,
      NonEmptyString(value).value,
      checked,
    )
end SqlSuite
