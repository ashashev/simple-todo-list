package simpletodolist.library

case class StorageId(id: Long) extends AnyVal

object StorageId {
  val ZERO = StorageId(0)
}
