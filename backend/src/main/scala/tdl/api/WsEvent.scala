package tdl.api

import io.circe.Encoder
import io.circe.Decoder
import io.circe.syntax.given
import io.circe.syntax.*

import tdl.model.*
import tdl.util.NonEmptyString

sealed trait WsEvent:
  def lid: ListId

case class ItemUpdated(lid: ListId, rid: RecordId, checked: Boolean)
    extends WsEvent

case class ListUpdated(lid: ListId, name: NonEmptyString, items: List[Record])
    extends WsEvent

object ItemUpdated:
  given Encoder[ItemUpdated] = Encoder.AsObject.derived[ItemUpdated]
  given Decoder[ItemUpdated] = Decoder.derived[ItemUpdated]
end ItemUpdated

object ListUpdated:
  given Encoder[ListUpdated] = Encoder.AsObject.derived[ListUpdated]
  given Decoder[ListUpdated] = Decoder.derived[ListUpdated]
end ListUpdated

object WsEvent:
  given Encoder[WsEvent] = Encoder.instance {
    case v: ItemUpdated => v.asJson
    case v: ListUpdated => v.asJson
  }

  given Decoder[WsEvent] = List[Decoder[WsEvent]](
    Decoder[ItemUpdated].map(identity),
    Decoder[ListUpdated].map(identity),
  ).reduceLeft(_ or _)
end WsEvent
