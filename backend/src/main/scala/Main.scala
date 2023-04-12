import cats.implicits.*

import tdl.util.*

@main def hello: Unit =
  println("Hello world!")
  println(msg)
  println(RecordId("wqe").show)
  println(ListId("wqe").show)

def msg = "I was compiled by Scala 3. :)"

def foo(id: RecordId): Unit =
  println(id.show)

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
    checked: Boolean = false,
)
