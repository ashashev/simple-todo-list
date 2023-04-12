package tdl.util

import cats.Show
import io.circe.Encoder

private trait BaseNewTyped[Raw]:

  opaque type Type = Raw

  private[util] def make(v: Raw): Type = v

  protected val innerFullName: String =
    getClass().getCanonicalName().stripSuffix("$")

  protected val innerShortName: String =
    getClass().getSimpleName().stripSuffix("$")

  given newTypeShow: Show[Type] = new Show[Type]:
    override def show(t: Type): String =
      s"${innerFullName}($t)"

  given toRaw: Conversion[Type, Raw] = _.toRaw

  given (using rawEnc: Encoder[Raw]): Encoder[Type] =
    rawEnc.contramap(_.toRaw)

  given (using ord: Ordering[Raw]): Ordering[Type] = new Ordering[Type]:
    def compare(x: Type, y: Type): Int = ord.compare(x.toRaw, y.toRaw)

  given (using rawPut: doobie.Put[Raw]): doobie.Put[Type] =
    rawPut.tcontramap(_.toRaw)

  extension (t: Type) def toRaw: Raw = t

end BaseNewTyped
