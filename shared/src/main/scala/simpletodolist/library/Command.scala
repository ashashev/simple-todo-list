package simpletodolist.library

object Command {
  def apply(raw: String): Command = {
    raw.split("\n").toList match {
      case "@GET@" :: _ => Get
      case "@UPD@" :: data => Update(Item(data.head.trim))
      case "@REP@" :: data => Replace(data.map(Item(_)))
      case _ => Unknown(raw)
    }
  }
}

trait Command {
  def toRaw: String
}

case object Get extends Command {
  override def toRaw = "@GET@\n"
}

sealed case class Update(i: Item) extends Command {
  override def toRaw = "@UPD@\n" + i.toRaw
}

sealed case class Replace(is: List[Item]) extends Command {
  override def toRaw = "@REP@\n" + (is map (_.toRaw) mkString "\n")
}

sealed case class Unknown(data: String) extends Command {
  override def toRaw: String = data
}