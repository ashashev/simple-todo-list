package tdl.util

import io.circe.Decoder

trait NewType[Raw] extends BaseNewType[Raw]:
  def apply(v: Raw): Type = make(v)

  given (using rawDec: Decoder[Raw]): Decoder[Type] =
    rawDec.map(make)

  given (using rawGet: doobie.Get[Raw]): doobie.Get[Type] =
    rawGet.tmap(make)
