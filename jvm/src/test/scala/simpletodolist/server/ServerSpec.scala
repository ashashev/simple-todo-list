package simpletodolist.server

import org.scalatest.{FlatSpecLike, Matchers}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe, WSTestRequestBuilding}
import simpletodolist.library

class FullTestKitExampleSpec extends FlatSpecLike with Matchers with ScalatestRouteTest {

  private trait TestServer {
    val testId = library.StorageId.toString(library.StorageId.ZERO)
    val rightUri = "/?id=" + testId
    val uriWithUnknownId = "/?id=400"
    val sm = system.actorOf(StorageManager.props(
      library.StorageInfo(library.StorageId.ZERO, "Test List") :: Nil
    ))
    val server = Server(sm)
  }

  private trait TestList {
    import library._
    val items = Item("one") :: Item("two") :: Item("three") :: Nil
  }

  val testCmdReplace = library.Replace(library.Item("one") :: library.Item("two") :: Nil)

  "The server" should "return 404 error for any request except WebSocket" in new TestServer {
    Get() ~> server.route ~> check {
      status shouldEqual StatusCodes.NotFound
    }
  }

  it should "return 404 error for the request without 'id' parameter" in new TestServer {
    val client = WSProbe()

    WSTestRequestBuilding.WS("/", client.flow) ~> server.route ~> check {
      status shouldEqual StatusCodes.NotFound
    }
  }

  it should "return 400 error for the request unknown storage" in new TestServer {
    val client = WSProbe()

    WSTestRequestBuilding.WS(uriWithUnknownId, client.flow) ~>
    server.route ~> check {
      status shouldEqual StatusCodes.BadRequest
    }
  }

  it should "return 101 code for correct request with known storage id" in new TestServer {
    val client = WSProbe()

    WSTestRequestBuilding.WS(rightUri, client.flow) ~> server.route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  it should "return the sent list" in new TestServer with TestList {
    val client = WSProbe()

    WSTestRequestBuilding.WS(rightUri, client.flow) ~> server.route ~> check {
      import library._
      client.sendMessage(Replace(items).toRaw)
      client.expectMessage(Replace(items).toRaw)
    }
  }

  it should "return the sent list by the Get command" in new TestServer with TestList {
    val client = WSProbe()

    WSTestRequestBuilding.WS(rightUri, client.flow) ~> server.route ~> check {
      import library.Replace
      client.sendMessage(Replace(items).toRaw)
      client.expectMessage()

      client.sendMessage(library.Get.toRaw)
      client.expectMessage(Replace(items).toRaw)
    }
  }
}


