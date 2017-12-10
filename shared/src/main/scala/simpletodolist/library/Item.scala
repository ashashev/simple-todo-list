package simpletodolist.library

import java.util.UUID


object Item {
  type Id = UUID

  def idFromString(sid: String): Id = UUID.fromString(sid)

  def createId(): Id = UUID.randomUUID()

  def apply(raw: String): Item = {
    val parts = raw.split(";").map(_.trim)

    assert(parts.size == 1 || parts.size == 3)

    if (parts.size == 3) new Item(idFromString(parts(0)), parts(1) == "+", parts(2))
    else if (parts(0)(0) == '+') new Item(createId(), true, parts(0).drop(1).trim())
    else new Item(createId(), false, parts(0))
  }
}

case class Item(id: Item.Id, checked: Boolean, text: String) {
  def toRaw = s"${id};${if (checked) "+" else "-"};${text}"
}
