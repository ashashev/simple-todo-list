package tdl.util

import cats.Show
import cats.data.NonEmptyList
import cats.data.ValidatedNel
import cats.implicits.given
import cats.syntax.*
import io.circe.Decoder

trait ValidatedNewType[Raw] extends BaseNewTyped[Raw]:
  /** Validation checks whether type can be constructed or not. It returns None
    * if it can be otherwise returns text description of error.
    */
  type Validation = Raw => Option[String]

  private type ErrorOr[A] = ValidatedNel[String, A]

  def apply(v: Raw): Either[NonEmptyList[String], Type] =
    validations.traverse(f => f(v)).map(_ => make(v)).toEither

  def maybe(v: Raw): Option[Type] = apply(v).toOption

  protected def addValidations(vs: Validation*): Unit =
    validations ++= vs.map { f => (v: Raw) =>
      f(v) match
        case None      => ().validNel
        case Some(err) => err.invalidNel
    }

  private var validations: Vector[Raw => ErrorOr[Unit]] =
    Vector.empty

  given (using rawDec: Decoder[Raw]): Decoder[Type] =
    rawDec.emap(apply(_).leftMap(_.intercalate("; ")))

  given (using rawGet: doobie.Get[Raw], rawShow: Show[Raw]): doobie.Get[Type] =
    rawGet.temap(apply(_).leftMap(_.intercalate("; ")))

end ValidatedNewType

object ValidatedNewType:

  trait NonEmptyString:
    self: ValidatedNewType[String] =>
    addValidations(s =>
      if s.trim().isEmpty() then
        s"${innerShortName} can't be created from empty string".some
      else None,
    )
  end NonEmptyString

end ValidatedNewType
