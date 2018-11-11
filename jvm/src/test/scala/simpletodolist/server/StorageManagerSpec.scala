package simpletodolist.server

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.{ActorSystem, ActorRef}
import akka.testkit.{TestKit, TestProbe}

import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import simpletodolist.library._

class StorageManagerSpec(_system: ActorSystem)
  extends TestKit(_system)
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("StorageManagerSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  def withSomeInfos(infos: Seq[StorageInfo])(testCode: Seq[StorageInfo] => Any) = {
    testCode(infos)
  }

  def withStorageManagerAndProbe(infos: Seq[StorageInfo])(testCode: (ActorRef, TestProbe) => Any) = {
    val sm = system.actorOf(StorageManager.props(infos), "StorageManager")
    val probe = TestProbe()

    try {
      testCode(sm, probe)
    } finally system.stop(sm)
  }

  "The storage manager" should "send back the storage list" in withSomeInfos(
      StorageInfo(StorageId(0), "Main") ::
      StorageInfo(StorageId(1), "First") ::
      Nil) { sl =>
    withStorageManagerAndProbe(sl) { (sm, probe) =>
      probe.send(sm, GetStorageList)
      probe.expectMsg(500 millis, sl)
    }
  }

  it should "send back the empty storage list" in withStorageManagerAndProbe(Seq()) { (sm, probe) =>
    probe.send(sm, GetStorageList)
    probe.expectMsg(500 millis, Seq[StorageInfo]())
  }

  it should "add new storage to the list" in withStorageManagerAndProbe(Seq()) { (sm, probe) =>
    probe.send(sm, AddStorage("first"))
    probe.send(sm, GetStorageList)
    val info = probe.expectMsgType[Seq[StorageInfo]](500 millis)
    assert(info !== null)
    assert(info.nonEmpty)
    assert(info.head.name === "first")
  }

  it should "remove storage from the list" in withSomeInfos(
      StorageInfo(StorageId(0), "Main") ::
      StorageInfo(StorageId(1), "First") ::
      Nil) { sl =>
    withStorageManagerAndProbe(sl) { (sm, probe) =>
      probe.send(sm, RemoveStorage(sl.tail.head.id))
      probe.send(sm, GetStorageList)
      probe.expectMsg(500 millis, Seq(sl.head))
    }
  }
}
