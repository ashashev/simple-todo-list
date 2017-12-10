package simpletodolist.server

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import simpletodolist.library._

import scala.concurrent.duration._
import scala.language.postfixOps

class ClientSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with FlatSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("ClientSpec"))

  "A client actor" should "send the join message to storage after getting the connected message" in {
    val storage = TestProbe()
    val outgoing = TestProbe()
    val client = system.actorOf(Client.props(storage.ref))
    client ! Client.Connected(outgoing.ref)
    storage expectMsg(500 millis, Storage.Join)
  }

  it should "send the disjoin message to storage after stop" in {
    val storage = TestProbe()
    val outgoing = TestProbe()
    val client = system.actorOf(Client.props(storage.ref))
    client ! Client.Connected(outgoing.ref)
    storage expectMsg(500 millis, Storage.Join)

    client ! PoisonPill
    storage expectMsg(500 millis, Storage.Disjoin)
  }

  it should "retransmits all expected messages from the outgoing to the storage" in {
    val todolist = Item(Item.createId(), false, "item 1") ::
      Item(Item.createId(), true, "item 2") ::
      Nil

    val storage = TestProbe()
    val outgoing = TestProbe()
    val client = system.actorOf(Client.props(storage.ref))
    client ! Client.Connected(outgoing.ref)
    storage expectMsg(500 millis, Storage.Join)

    outgoing send(client, Get)
    storage expectMsg(500 millis, Get)

    outgoing send(client, Replace(todolist))
    storage expectMsg(500 millis, Replace(todolist))

    outgoing send(client, Update(todolist.head))
    storage expectMsg(500 millis, Update(todolist.head))
  }

  it should "retransmits all expected messages from the storage to the outgoing" in {
    val todolist = Item(Item.createId(), false, "item 1") ::
      Item(Item.createId(), true, "item 2") ::
      Nil

    val storage = TestProbe()
    val outgoing = TestProbe()
    val client = system.actorOf(Client.props(storage.ref))
    client ! Client.Connected(outgoing.ref)
    storage expectMsg(500 millis, Storage.Join)

    storage send(client, Get)
    outgoing expectMsg(500 millis, Get)

    storage send(client, Replace(todolist))
    outgoing expectMsg(500 millis, Replace(todolist))

    storage send(client, Update(todolist.head))
    outgoing expectMsg(500 millis, Update(todolist.head))
  }
}
