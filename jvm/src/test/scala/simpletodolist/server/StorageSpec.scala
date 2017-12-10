package simpletodolist.server

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import simpletodolist.library._
import simpletodolist.server.Storage.{Disjoin, Join}

import scala.concurrent.duration._
import scala.language.postfixOps

class StorageSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with FlatSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("StorageSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "A Storage Actor" should "pass on a Replace message when instructed to" in {
    val testProbe = TestProbe()
    val storage = system.actorOf(Storage.props(List.empty))
    testProbe send(storage, Join)
    testProbe send(storage, Get)
    testProbe.expectMsg(500 millis, Replace(List.empty))
  }

  it should "pass on a Replace message with non empty list when instructed to" in {
    val testProbe = TestProbe()
    val todolist = Item(Item.createId(), false, "item 1") ::
      Item(Item.createId(), true, "item 2") ::
      Nil
    val storage = system.actorOf(Storage.props(todolist))
    testProbe send(storage, Join)
    testProbe send(storage, Get)
    testProbe.expectMsg(500 millis, Replace(todolist))
  }

  it should "update a TODO list and pass on an Update message to the clients when instructed to" in {
    val todolist = Item(Item.createId(), false, "item 1") ::
      Item(Item.createId(), true, "item 2") ::
      Nil

    val storage = system.actorOf(Storage.props(todolist))
    val probe1 = TestProbe()
    val probe2 = TestProbe()

    probe1 send(storage, Join)
    probe2 send(storage, Join)

    val updatedTodolist = todolist.head.copy(checked = true) :: todolist.tail
    val updCmd = Update(todolist.head.copy(checked = true))
    probe1 send(storage, updCmd)

    probe1 expectMsg(500 millis, updCmd)
    probe2 expectMsg(500 millis, updCmd)

    probe2 send(storage, Get)
    probe2 expectMsg(500 millis, Replace(updatedTodolist))
  }

  it should "not send Update messages after disjoin" in {
    val todolist = Item(Item.createId(), false, "item 1") ::
      Item(Item.createId(), true, "item 2") ::
      Nil

    val storage = system.actorOf(Storage.props(todolist))

    val probe1 = TestProbe()
    val probe2 = TestProbe()

    probe1 send(storage, Join)
    probe2 send(storage, Join)

    val updCmd1 = Update(todolist.head.copy(checked = true))
    probe2 send(storage, updCmd1)
    probe1 expectMsg(500 millis, updCmd1)
    probe2 expectMsg(500 millis, updCmd1)

    probe1 send(storage, Disjoin)

    val updCmd2 = Update(todolist.head)
    probe2 send(storage, updCmd2)
    probe2 expectMsg(500 millis, updCmd2)
    probe1 expectNoMsg(1 second)
  }

  it should "replace a TODO list and pass on a Replace message to the clients when instructed to" in {
    val todolist1 = Item(Item.createId(), false, "item 1-1") ::
      Item(Item.createId(), true, "item 1-2") ::
      Nil

    val storage = system.actorOf(Storage.props(todolist1))

    val probe1 = TestProbe()
    val probe2 = TestProbe()

    probe1 send(storage, Join)
    probe2 send(storage, Join)

    val todolist2 = Item(Item.createId(), false, "item 2-1") ::
      Item(Item.createId(), true, "item 2-2") ::
      Nil

    probe1 send(storage, Replace(todolist2))
    probe1 expectMsg(500 millis, Replace(todolist2))
    probe2 expectMsg(500 millis, Replace(todolist2))

    probe2 send(storage, Get)
    probe2 expectMsg(500 millis, Replace(todolist2))
  }

  it should "add new item into the TODO list and pass on an Update message to the clients when instructes to" in {
    val todolist1 = Item(Item.createId(), false, "item 1-1") ::
      Item(Item.createId(), true, "item 1-2") ::
      Nil
    val storage = system.actorOf(Storage.props(todolist1))

    val probe1 = TestProbe()

    probe1 send (storage, Join)

    val item = Item(Item.createId(), false, "item 1-3")
    val todolist2 = item :: todolist1

    probe1 send (storage, Update(item))
    probe1 expectMsg(500 millis, Update(item))

    probe1 send (storage, Get)
    probe1 expectMsg(500 millis, Replace(todolist2))
  }
}
