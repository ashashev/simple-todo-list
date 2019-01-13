package simpletodolist.server

import akka.actor.{Actor, ActorRef, Props}

import simpletodolist.library._

object StorageManager {
  def props(ss: List[StorageInfo]) = Props(new StorageManager(ss))

  case object GetStorageList
  case class AddStorage(name: String)
  case class RemoveStorage(id: StorageId)
  case class GetStorage(id: StorageId)
}

class StorageManager(private[this] var info: List[StorageInfo]) extends Actor {
  import StorageManager._

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
    case GetStorage(id) => storages.get(id) match {
      case Some(ref) => sender() ! Option(ref)
      case None if info.exists(_.id == id) =>
        val ref = context.system.actorOf(Storage.props(List(), id))
        storages += id -> ref
        sender() ! Option(ref)
      case _ =>
        sender() ! Option.empty
    }
  }

}
