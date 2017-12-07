package simpletodolist.server

import akka.actor.{Actor, ActorRef, Props}
import simpletodolist.library._

object Storage {
  def props(items: List[Item]) = Props(new Storage(items))
  case class Join(listId: String)
  case class Disjoin(listId: String)
}

class Storage(var items_ : List[Item]) extends Actor {
  import Storage._

  var listIdToClients = Map.empty[String, Set[ActorRef]].
    withDefault(_ => Set.empty[ActorRef])
  var clientToListId = Map.empty[ActorRef, String]

  var items = Map.empty[String, List[Item]]

  override def receive: Receive = {
    case Join(listId) =>
      listIdToClients = listIdToClients updated (listId, listIdToClients(listId) + sender())
      clientToListId += sender() -> listId

    case Disjoin(listId) =>
      val newClients = listIdToClients(listId) - sender()
      listIdToClients =
        if (newClients.isEmpty) listIdToClients - listId
        else listIdToClients updated(listId, newClients)

    case Get =>
      val id = clientToListId(sender())
      sender() ! Replace(items(id))

    case Replace(newItems) =>
      val id = clientToListId(sender())
      items updated (id, newItems)
      for (client <- listIdToClients(id)) {
        client ! Replace(newItems)
      }

    case cmd@Update(newItem) =>
      val id = clientToListId(sender())
      val is = items(id)
      val ind = is.indexWhere(newItem.id == _.id)
      items updated(id,
        if (ind != -1) is.updated(ind, newItem)
        else newItem :: is
      )
      for (client <- listIdToClients(id)) {
        client ! cmd
      }
  }
}
