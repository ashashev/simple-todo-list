package tdl

import cats.effect.Concurrent
import cats.implicits.*
import io.circe.Decoder
import io.circe.Encoder
import org.http4s.*
import org.http4s.circe.*

import tdl.model.*
import tdl.util.NonEmptyString

package object api:

  case class ListInfo(lid: ListId, name: NonEmptyString)
  object ListInfo:
    given Encoder[ListInfo] = Encoder.AsObject.derived[ListInfo]
    given Decoder[ListInfo] = Decoder.derived[ListInfo]

    given [F[_]: Concurrent]: EntityEncoder[F, ListInfo] = jsonEncoderOf
    given [F[_]: Concurrent]: EntityDecoder[F, ListInfo] = jsonOf
  end ListInfo

  case class TodoList(lid: ListId, name: NonEmptyString, items: List[Record])
  object TodoList:
    given Encoder[TodoList] = Encoder.AsObject.derived[TodoList]
    given Decoder[TodoList] = Decoder.derived[TodoList]

    given [F[_]: Concurrent]: EntityEncoder[F, TodoList] = jsonEncoderOf
    given [F[_]: Concurrent]: EntityDecoder[F, TodoList] = jsonOf
  end TodoList

  case class TodoListData(name: NonEmptyString, items: List[Record])
  object TodoListData:
    given Encoder[TodoListData] = Encoder.AsObject.derived[TodoListData]
    given Decoder[TodoListData] = Decoder.derived[TodoListData]

    given [F[_]: Concurrent]: EntityEncoder[F, TodoListData] = jsonEncoderOf
    given [F[_]: Concurrent]: EntityDecoder[F, TodoListData] = jsonOf
  end TodoListData

  case class ItemUpdate(checked: Boolean)
  object ItemUpdate:
    given Encoder[ItemUpdate] = Encoder.AsObject.derived[ItemUpdate]
    given Decoder[ItemUpdate] = Decoder.derived[ItemUpdate]

    given [F[_]: Concurrent]: EntityEncoder[F, ItemUpdate] = jsonEncoderOf
    given [F[_]: Concurrent]: EntityDecoder[F, ItemUpdate] = jsonOf
  end ItemUpdate

end api
