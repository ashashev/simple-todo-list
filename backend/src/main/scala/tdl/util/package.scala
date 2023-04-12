package tdl

import util.*

package object util:

  object NonEmptyString
      extends ValidatedNewType[String]
      with ValidatedNewType.NonEmptyString

  type NonEmptyString = NonEmptyString.Type
