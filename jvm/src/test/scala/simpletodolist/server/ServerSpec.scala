package simpletodolist.server

import org.scalatest.{FlatSpecLike, Matchers}
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe, WSTestRequestBuilding}
import akka.testkit.TestProbe
import scala.concurrent.duration._
import simpletodolist.library

class FullTestKitExampleSpec extends FlatSpecLike with Matchers with ScalatestRouteTest {

  val testCmdReplace = library.Replace(library.Item("one") :: library.Item("two") :: Nil)

  "The server" should "return 404 error for any request except WebSocket" in {
    val fakeStorage = TestProbe()
    val server = Server(fakeStorage.ref)

    Get() ~> server.route ~> check {
      status shouldEqual StatusCodes.NotFound
    }
  }

  "The server" should "routes commands from WebSocket to the storage" in {
    val fakeStorage = TestProbe()
    val fakeClient = WSProbe()
    val server = Server(fakeStorage.ref)

    WSTestRequestBuilding.WS("/", fakeClient.flow) ~> server.route ~> check {
      fakeStorage.expectMsg(500.millis, Storage.Join)

      fakeClient.sendMessage(library.Get.toRaw)
      fakeStorage.expectMsg(500.millis, library.Get)

      fakeClient.sendMessage(testCmdReplace.toRaw)
      fakeStorage.expectMsg(500.millis, testCmdReplace)

    }
  }

  it should "routes commands from streamed WebSocket to the storage" in {
    val fakeStorage = TestProbe()
    val fakeClient = WSProbe()
    val server = Server(fakeStorage.ref)

    WSTestRequestBuilding.WS("/", fakeClient.flow) ~> server.route ~> check {
      fakeStorage.expectMsg(500.millis, Storage.Join)

      import akka.http.scaladsl.model.ws.TextMessage.Streamed

      fakeClient.sendMessage(Streamed(Source.single(library.Get.toRaw)))
      fakeStorage.expectMsg(500.millis, library.Get)

      fakeClient.sendMessage(Streamed(
        Source(testCmdReplace.toRaw.split('\n').map(_ + "\n").toList)
      ))
      fakeStorage.expectMsg(500.millis, testCmdReplace)
    }
  }

  it should "routes command from the storage to the client through WebSocket" in {
    val fakeStorage = TestProbe()
    val fakeClient = WSProbe()
    val server = Server(fakeStorage.ref)

    WSTestRequestBuilding.WS("/", fakeClient.flow) ~> server.route ~> check  {
      fakeStorage.expectMsg(500.millis, Storage.Join)
      val client = fakeStorage.lastSender

      fakeStorage.send(client, testCmdReplace)
      fakeClient.expectMessage(testCmdReplace.toRaw)
    }
  }

}


