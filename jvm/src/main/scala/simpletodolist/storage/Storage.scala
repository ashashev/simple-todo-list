package simpletodolist.storage

import simpletodolist.library.Item
import simpletodolist.library.ListId

trait Storage[F[_]] {

  def subscribe(callback: Event => Any): Subscription

  def get(list: ListId): F[Option[List[Item]]]

  def update(list: ListId, item: Item): F[Unit]

  def replace(list: ListId, items: List[Item]): F[Unit]
}
