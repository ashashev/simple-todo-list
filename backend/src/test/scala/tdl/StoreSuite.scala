package tdl

import scala.concurrent.duration._

import cats.effect.*
import cats.effect.kernel.Outcome.Succeeded
import cats.instances.all.given
import cats.syntax.all.given
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import munit.CatsEffectSuite

import tdl.api.ItemUpdated
import tdl.api.ListUpdated
import tdl.model.ListId
import tdl.model.Record
import tdl.model.RecordId
import tdl.model.TodoStore
import tdl.util.NonEmptyString
import tdl.tests.munit.Ops.*

class StoreSuite extends CatsEffectSuite:

  test("broadcast changes") {
    val lid = ListId("list-1").value
    val name = NonEmptyString("ToDo #1").value

    val rs1 = Vector(
      Record(
        RecordId("zcx").value,
        value = NonEmptyString("item").value,
        checked = false,
      ),
      Record(
        RecordId("asd").value,
        value = NonEmptyString("another item").value,
        checked = false,
      ),
    )

    val rs2 = Vector(
      Record(
        RecordId("dfg").value,
        value = NonEmptyString("new item").value,
        checked = false,
      ),
      Record(
        RecordId("ert").value,
        value = NonEmptyString("one more item").value,
        checked = false,
      ),
    )

    val expected1 = Vector(
      ListUpdated(lid, name, rs1.toList),
      ItemUpdated(lid, rs1(1).id, true),
      ItemUpdated(lid, rs1(0).id, true),
      ListUpdated(lid, name, rs2.toList),
    )

    val expected2 = Vector(
      ListUpdated(
        lid,
        name,
        rs1.updated(1, rs1(1).copy(checked = true)).toList,
      ),
      ItemUpdated(lid, rs1(0).id, true),
      ListUpdated(lid, name, rs2.toList),
    )

    for
      intSig <- SignallingRef[IO].of(false)
      store <- Store[IO](TodoStore.memEmpty)
      _ <- store.replace(lid, name, rs1.toList)
      s1 = store.subscribe(lid)
      f1 <- s1.interruptWhen(intSig).compile.toVector.start
      _ <- IO.sleep(300.millis)
      _ <- store.update(lid, rs1(1).id, true)
      s2 = store.subscribe(lid)
      f2 <- s2.interruptWhen(intSig).compile.toVector.start
      _ <- IO.sleep(300.millis)
      _ <- store.update(lid, rs1(0).id, true)
      _ <- store.replace(lid, name, rs2.toList)
      _ <- IO.sleep(300.millis)
      _ <- intSig.set(true)
      results <- (f1, f2).toList.traverse(_.join)
      got <- results.traverseCollect { case Succeeded(v) => v }
      _ <- IO(assertEquals(got.size, 2))
      evs1 = got.head
      evs2 = got(1)
      _ <- IO(assertEquals(evs1, expected1))
      _ <- IO(assertEquals(evs2, expected2))
    yield ()
  }

  test("get lists") {
    val lid1 = ListId("list-1").value
    val name1 = NonEmptyString("ToDo #1").value

    val rs1 = Vector(
      Record(
        RecordId("zcx").value,
        value = NonEmptyString("item").value,
        checked = false,
      ),
    )

    val lid2 = ListId("list-2").value
    val name2 = NonEmptyString("ToDo #2").value

    val rs2 = Vector(
      Record(
        RecordId("asd").value,
        value = NonEmptyString("another item").value,
        checked = false,
      ),
    )

    for
      store <- Store[IO](TodoStore.memEmpty)
      _ <- store.replace(lid1, name1, rs1.toList)
      _ <- store.replace(lid2, name2, rs2.toList)
      lists <- store.getLists()
      _ <- IO(
        assertEquals(lists.sorted, List(lid1 -> name1, lid2 -> name2).sorted),
      )
    yield ()
  }

end StoreSuite

object StoreSuite:
end StoreSuite
