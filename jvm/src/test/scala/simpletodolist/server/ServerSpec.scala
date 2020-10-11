package simpletodolist.server

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe, WSTestRequestBuilding}
import akka.stream.scaladsl.Source
import akka.testkit.TestProbe
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import simpletodolist.library
import simpletodolist.storage._

class FullTestKitExampleSpec extends AnyFlatSpecLike with Matchers with ScalatestRouteTest {

  val testCmdReplace = library.Replace(library.Item("one") :: library.Item("two") :: Nil)

  class FakeStorage(initial: List[library.Item] = Nil) extends Storage[Future] {
    @volatile private var cb: Option[Event => Any] = None

    private val items: TrieMap[library.ItemId, library.Item] =
      TrieMap.empty.addAll(initial.iterator.map(i => i.id -> i))

    override def subscribe(callback: Event => Any): Subscription = {
      cb = Some(callback)

      new Subscription {
        override def cancel(): Unit = cb = None
      }
    }

    override def get(list: library.ListId): Future[Option[List[library.Item]]] =
      Future.successful(Some(items.values.toList))

    override def update(list: library.ListId, item: library.Item): Future[Unit] =
      Future.successful {
        items(item.id) = item
        cb.foreach(_.apply(Updated(list, item)))
      }

    override def replace(list: library.ListId, newItems: List[library.Item]): Future[Unit] =
      Future.successful {
        items.clear()
        items.addAll(newItems.iterator.map(i => i.id -> i))
        cb.foreach(_.apply(Replaced(list, newItems)))
      }

  }

  "The server" `should` "return 404 error for any request except WebSocket" in {
    val fakeStorage = new FakeStorage()
    val server = Server(fakeStorage)

    Get() ~> server.route ~> check {
      status `shouldEqual` StatusCodes.NotFound
    }
  }

  "The server" `should` "routes commands from WebSocket to the storage" in {
    val fakeClient = WSProbe()
    val fakeStorage = new FakeStorage()
    val server = Server(fakeStorage)

    WSTestRequestBuilding.WS("/", fakeClient.flow) ~> server.route ~> check {
      info("Get")
      fakeClient.sendMessage(library.Get.toRaw)
      fakeClient.expectMessage(library.Replace(Nil).toRaw)

      info("Replace")
      fakeClient.sendMessage(testCmdReplace.toRaw)
      fakeClient.expectMessage(testCmdReplace.toRaw)
    }
  }

  it `should` "routes commands from streamed WebSocket to the storage" in {
    val fakeClient = WSProbe()
    val fakeStorage = new FakeStorage()
    val server = Server(fakeStorage)

    WSTestRequestBuilding.WS("/", fakeClient.flow) ~> server.route ~> check {
      import akka.http.scaladsl.model.ws.TextMessage.Streamed

      info("Get")
      fakeClient.sendMessage(Streamed(Source.single(library.Get.toRaw)))
      fakeClient.expectMessage(library.Replace(Nil).toRaw)

      info("Replace")
      fakeClient.sendMessage(Streamed(
        Source(testCmdReplace.toRaw.split('\n').map(_ + "\n").toList)
      ))
      fakeClient.expectMessage(testCmdReplace.toRaw)
    }
  }

  it `should` "routes command from the storage to the client through WebSocket" in {
    val fakeClient = WSProbe()
    val fakeStorage = new FakeStorage()
    val server = Server(fakeStorage)

    WSTestRequestBuilding.WS("/", fakeClient.flow) ~> server.route ~> check  {
      fakeStorage.update(Server.DEFAULT_LIST, testCmdReplace.is.head)
      fakeClient.expectMessage(library.Update(testCmdReplace.is.head).toRaw)
    }
  }

}


