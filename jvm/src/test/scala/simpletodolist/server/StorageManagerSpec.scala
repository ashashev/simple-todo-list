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

  val testSet = StorageInfo(StorageId(0), "Main") ::
    StorageInfo(StorageId(1), "First") :: Nil

  def withSomeInfos(infos: List[StorageInfo])(testCode: List[StorageInfo] => Any) = {
    testCode(infos)
  }

  def withStorageManagerAndProbe(infos: List[StorageInfo])(testCode: (ActorRef, TestProbe) => Any) = {
    val sm = system.actorOf(StorageManager.props(infos), "StorageManager")
    val probe = TestProbe()

    try {
      testCode(sm, probe)
    } finally system.stop(sm)
  }

  "The storage manager" should "send back the storage list" in withSomeInfos(
      testSet) { sl =>
    withStorageManagerAndProbe(sl) { (sm, probe) =>
      probe.send(sm, GetStorageList)
      probe.expectMsg(500 millis, sl)
    }
  }

  it should "send back the empty storage list" in withStorageManagerAndProbe(List()) { (sm, probe) =>
    probe.send(sm, GetStorageList)
    probe.expectMsg(500 millis, List[StorageInfo]())
  }

  it should "add new storage to the list" in withStorageManagerAndProbe(List()) { (sm, probe) =>
    probe.send(sm, AddStorage("first"))
    probe.send(sm, GetStorageList)
    val info = probe.expectMsgType[List[StorageInfo]](500 millis)
    assert(info !== null)
    assert(info.nonEmpty)
    assert(info.head.name === "first")
  }

  it should "remove storage from the list" in withSomeInfos(
      testSet) { sl =>
    withStorageManagerAndProbe(sl) { (sm, probe) =>
      probe.send(sm, RemoveStorage(sl.tail.head.id))
      probe.send(sm, GetStorageList)
      probe.expectMsg(500 millis, List(sl.head))
    }
  }

  it should "send back None if the requested storage is unknown" in withSomeInfos(
    testSet) { sl =>
    withStorageManagerAndProbe(sl) { (sm, probe) =>

      probe.send(sm, StorageManager.GetStorage(StorageId(-1)))
      probe.expectMsg(500 millis, None)
    }
  }

  it should "send back Some(ActorRef) as answer on the GetStorage" in withSomeInfos(
    testSet) { sl =>
    withStorageManagerAndProbe(sl) { (sm, probe) =>

      val id = sl.drop(1).head.id
      probe.send(sm, StorageManager.GetStorage(id))
      val ref = probe.expectMsgType[Option[ActorRef]](500 millis)

      assert(ref.nonEmpty)
      probe.send(ref.get, Storage.GetId)
      probe.expectMsg(500 millis, id)
    }
  }

  it should "send back reference to the same storage as answer on the same ID" in withSomeInfos(
    testSet) { sl =>
    withStorageManagerAndProbe(sl) { (sm, probe) =>
      val id1 = sl.head.id
      val id2 = sl.drop(1).head.id

      probe.send(sm, StorageManager.GetStorage(id1))
      val s1 = probe.expectMsgType[Option[ActorRef]](500 millis)

      probe.send(sm, StorageManager.GetStorage(id2))
      val s2 = probe.expectMsgType[Option[ActorRef]](500 millis)

      probe.send(sm, StorageManager.GetStorage(id1))
      val s3 = probe.expectMsgType[Option[ActorRef]](500 millis)

      assert(s1 !== s2)
      assert(s1 === s3)
    }
  }
}
