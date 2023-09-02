import cats.effect.*
import cats.instances.all.given
import cats.syntax.all.given
import munit.CatsEffectSuite

class MyEffectfulSuite extends CatsEffectSuite:

  val incNumber = 100
  val fibersNumber = 10

  def fibers(ref: Ref[IO, Int], inc: Ref[IO, Int] => IO[Unit]) =
    (1 to fibersNumber).toList.traverse(_ => inc(ref).start)

  def incBad(ref: Ref[IO, Int]): IO[Unit] =
    (1 to incNumber).toList
      .traverse(_ => ref.get.flatMap(v => ref.set(v + 1)))
      .as(())

  def incGood(ref: Ref[IO, Int]): IO[Unit] =
    (1 to incNumber).toList
      .traverse(_ => ref.update(_ + 1))
      .as(())

  test("concurrent ref bad") {
    val got = for
      ref <- Ref[IO].of(0)
      fs <- fibers(ref, incBad)
      _ <- fs.traverse(_.join)
      v <- ref.get
      _ <- IO(println(munitPrint(s"result: $v")))
    yield v

    got.map(a => assertNotEquals(a, incNumber * fibersNumber))
  }

  test("concurrent ref good") {
    val got = for
      ref <- Ref[IO].of(0)
      fs <- fibers(ref, incGood)
      _ <- fs.traverse(_.join)
      v <- ref.get
    yield v

    got.map(a => assertEquals(a, incNumber * fibersNumber))
  }

end MyEffectfulSuite
