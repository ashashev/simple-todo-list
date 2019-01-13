package simpletodolist.library

case class StorageId(id: Long) extends AnyVal

object StorageId {
  val ZERO = StorageId(0)

  def fromString(s: String): StorageId = new StorageId(s.toLong)
  def toString(i: StorageId): String = i.id.toString
}
