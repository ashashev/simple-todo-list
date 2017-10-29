package simpletodolist.server

import akka.actor.{Actor, ActorRef, Props}
import simpletodolist.library._

object Storage {
  def props(items: List[Item]) = Props(new Storage(items))
  case object Join
  case object Disjoin
}

class Storage(var items: List[Item]) extends Actor {
  import Storage._

  var clients = Set.empty[ActorRef]

  override def receive: Receive = {
    case Join =>
      clients += sender()
    case Disjoin =>
      clients -= sender()
    case Get =>
      sender() ! Replace(items)
    case Replace(newItems) =>
      items = newItems
      for (client <- clients) {
        client ! Replace(items)
      }
    case cmd@Update(newItem) =>
      val ind = items.indexWhere(newItem.id == _.id)
      items = if (ind != -1) items.updated(ind, newItem)
              else newItem :: items
      for (client <- clients) {
        client ! cmd
      }
  }
}
