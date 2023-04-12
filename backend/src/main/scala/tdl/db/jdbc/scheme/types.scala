package tdl.db.jdbc.scheme

import tdl.model
import tdl.util.NonEmptyString

case class Item(
    lid: model.ListId,
    id: model.RecordId,
    order: Int,
    value: NonEmptyString,
    checked: Boolean,
)
