package tdl.util

import cats.data.NonEmptyList
import cats.syntax.either.*

import tdl.tests.munit.Ops.*

class ValidatedNewTypeSuite extends munit.FunSuite:

  import ValidatedNewTypeSuite._

  test("all validation are used") {
    val expectedErrs = NonEmptyList.of(errBelowOne, errMustBeOdd)
    assertEquals(OddNumbersGreatZero(-2), expectedErrs.asLeft)
  }

  test("succeed creation and toRaw") {
    val base = 7
    val got = OddNumbersGreatZero(base).value
    assertEquals(got.toRaw, base)
  }

  def creatingSomeNonEmptyStringFailsWhenInputIs(label: String, input: String)(
      using loc: munit.Location,
  ): Unit =
    test(s"creating NonEmptyString fails when input is $label") {
      val got = SomeNonEmptyString(input)
      assertEquals(
        got,
        NonEmptyList
          .of("SomeNonEmptyString can't be created from empty string")
          .asLeft,
      )
    }

  creatingSomeNonEmptyStringFailsWhenInputIs("empty string", "")

  creatingSomeNonEmptyStringFailsWhenInputIs("'whitespace' symbols", "\t\r\n ")

end ValidatedNewTypeSuite

object ValidatedNewTypeSuite:

  val errBelowOne = "below one"
  val errMustBeOdd = "must be odd"

  object OddNumbersGreatZero extends ValidatedNewType[Int]:
    addValidations(
      x => if x < 1 then Some(errBelowOne) else None,
      x => if x % 2 == 0 then Some(errMustBeOdd) else None,
    )
  end OddNumbersGreatZero

  type OddNumbersGreatZero = OddNumbersGreatZero.Type

  object SomeNonEmptyString
      extends ValidatedNewType[String]
      with ValidatedNewType.NonEmptyString

  type SomeNonEmptyString = SomeNonEmptyString.Type

end ValidatedNewTypeSuite
