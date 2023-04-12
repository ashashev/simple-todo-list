package tdl.tests.munit

import munit.Assertions
import cats.effect.IO
import cats.effect.Outcome
import cats.MonadThrow
import cats.ApplicativeThrow
import cats.implicits.*
import cats.syntax.all.given

object Ops:
  extension [M[_]: MonadThrow, A](io: M[Outcome[M, Throwable, A]])
    def value(using loc: munit.Location): M[A] =
      io.flatMap {
        case Outcome.Succeeded(fa) => fa
        case x =>
          MonadThrow[M].catchNonFatal(
            Assertions.fail(s"expect Succeeded but got: $x"),
          )
      }
  end extension

  extension [L, R](v: Either[L, R])
    def value(using loc: munit.Location): R =
      v match {
        case Right(a) => a
        case Left(a)  => Assertions.fail(s"expect Right but got Left with $a")
      }
  end extension
