package simpletodolist.storage

import simpletodolist.library.Item
import simpletodolist.library.ListId

sealed trait Event extends Product with Serializable

case class Updated(list: ListId, item: Item) extends Event

case class Replaced(list: ListId, items: List[Item]) extends Event

