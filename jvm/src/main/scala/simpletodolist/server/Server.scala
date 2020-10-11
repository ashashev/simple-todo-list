package simpletodolist.server

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.stream.Materializer
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.BroadcastHub
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

import simpletodolist.library.Command
import simpletodolist.library.Get
import simpletodolist.library.Item
import simpletodolist.library.ListId
import simpletodolist.library.Replace
import simpletodolist.library.Update
import simpletodolist.storage.Event
import simpletodolist.storage.Replaced
import simpletodolist.storage.Storage
import simpletodolist.storage.Updated

class Server(storage: Storage[Future])( using materializer: Materializer, ec: ExecutionContext)
    extends Routes {

  import Server._

  val (storageEventsQueue, storageEventsSource) =
    Source.queue[Event](100, OverflowStrategy.fail).toMat(BroadcastHub.sink)(Keep.both).run()

  private val subscription = storage.subscribe(evt => storageEventsQueue.offer(evt))
  storageEventsSource.runWith(Sink.ignore)

  def pipeline: Flow[Command, Command, Any] =
    Flow[Command].mapAsync[Option[List[Item]] | Unit](1) {
      case Get => storage.get(DEFAULT_LIST)
      case Update(item) => storage.update(DEFAULT_LIST, item)
      case Replace(items) => storage.replace(DEFAULT_LIST, items)
    }
    .collect[Command] {
      case Some(items) => Replace(items)
      case None => Replace(Nil)
    }
    .mergePreferred(
      storageEventsSource.map {
        case Updated(_, item) => Update(item)
        case Replaced(_, items) => Replace(items)
      },
      priority = false,
      eagerComplete = true
    )
}

object Server {
  val DEFAULT_LIST: ListId = "default-list"
}
