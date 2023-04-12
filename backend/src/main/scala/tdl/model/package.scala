package tdl

import io.circe.Decoder
import io.circe.Encoder

import tdl.util.*

package object model:

  object ListId
      extends ValidatedNewType[String]
      with ValidatedNewType.NonEmptyString
  type ListId = ListId.Type

  object RecordId
      extends ValidatedNewType[String]
      with ValidatedNewType.NonEmptyString
  type RecordId = RecordId.Type

  final case class Record(
      id: RecordId,
      value: NonEmptyString,
      checked: Boolean,
  )
  object Record:
    given Encoder[Record] = Encoder.AsObject.derived[Record]
    given Decoder[Record] = Decoder.derived[Record]
  end Record

end model
