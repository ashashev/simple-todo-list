package simpletodolist.storage

trait Subscription {
  def cancel(): Unit
}
