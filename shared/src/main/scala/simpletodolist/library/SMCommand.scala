package simpletodolist.library

object SMCommand

trait SMCommand

case object GetStorageList extends SMCommand
case class AddStorage(name: String) extends SMCommand
case class RemoveStorage(id: StorageId) extends SMCommand
