package simpletodolist.server

import akka.actor.{Actor, ActorRef, Props}

import simpletodolist.library._

object StorageManager {
  def props(ss: Seq[StorageInfo]) = Props(new StorageManager(ss))

  case class GetStorage(id: StorageId)
}

class StorageManager(private[this] var info: Seq[StorageInfo]) extends Actor {
  private[this] var lastId = (StorageId.ZERO /: info){(id, info) =>
    StorageId(id.id max info.id.id)}

  private[this] var storages = Map.empty[StorageId, ActorRef]

  private[this] def getNextId(): StorageId = {
    lastId = StorageId(lastId.id + 1)
    lastId
  }

  override def receive: Receive = {
    case GetStorageList =>
      sender() ! info
    case AddStorage(name) =>
      info = StorageInfo(getNextId(), name) +: info
    case RemoveStorage(id) =>
      info = info.filterNot(_.id == id)
    case _ => ()
  }

}
