package simpletodolist.library

object Command {
  def apply(raw: String): Command = {
    raw.split("\n").toList match {
      case "@GET@" :: _ => Get
      case "@UPD@" :: data => Update(Item(data.head))
      case "@REP@" :: data => Replace(data.map(Item(_)))
      case _ => throw new Error("Can't parse command string - unknown command: " + raw)
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
