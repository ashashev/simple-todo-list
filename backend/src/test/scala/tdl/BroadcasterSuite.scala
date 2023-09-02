package tdl

import scala.concurrent.duration._

import cats.effect.*
import cats.effect.kernel.Outcome.Succeeded
import cats.effect.std.Random
import cats.instances.all.given
import cats.syntax.all.given
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import munit.CatsEffectSuite

class BroadcasterSuite extends CatsEffectSuite:
  import BroadcasterSuite._

  def checkSingleWriter(
      name: String,
      makeBroadcaster: () => IO[Broadcaster[IO, Int]],
  )(using
      loc: munit.Location,
  ): Unit =
    test(s"Single writer: $name") {
      for
        rand <- Random.scalaUtilRandom[IO]
        data <- randData(rand)(10)
        intSig <- SignallingRef[IO].of(false)
        b <- makeBroadcaster()

        readers <- (1 to 10).toList.traverse(_ => runReader(b)(intSig))
        _ <- IO.sleep(100.millis)
        writer <- runWriter(b)(data)
        _ <- writer.join
        _ <- IO.sleep(100.millis)
        _ <- intSig.set(true)
        results <- readers.traverse(_.join)

        got <- results.traverseCollect { case Succeeded(fa) => fa }
        _ <- got.traverse(r => IO(assertEquals(r, data)))
      yield ()
    }

  def checkMultipleWriters(
      name: String,
      makeBroadcaster: () => IO[Broadcaster[IO, Int]],
  )(using
      loc: munit.Location,
  ): Unit =
    test(s"Multiple writers: $name") {
      val writersNum = 5
      val messageNum = 5

      for
        rand <- Random.scalaUtilRandom[IO]
        intSig <- SignallingRef[IO].of(false)
        b <- makeBroadcaster()

        readers <- (1 to 10).toList.traverse(_ => runReader(b)(intSig))
        _ <- IO.sleep(100.millis)
        writers <- (1 to writersNum).toList.traverse(_ =>
          randData(rand)(messageNum).flatMap(runWriter(b)),
        )
        _ <- writers.traverse(_.join)
        _ <- IO.sleep(100.millis)
        _ <- intSig.set(true)
        results <- readers.traverse(_.join)

        got <- results.traverseCollect { case Succeeded(fa) => fa }
        _ <- got.traverse(r =>
          IO(assertEquals(r.size, writersNum * messageNum)),
        )
        x = got.head
        rs = got.tail
        _ <- rs.traverse(r => IO(assertEquals(r, x)))
      yield ()
    }

  checkSingleWriter("BroadcasterSimple", () => Broadcaster.simple[IO, Int])
  checkMultipleWriters("BroadcasterSimple", () => Broadcaster.simple[IO, Int])

  checkSingleWriter("BroadcasterTopic", () => Broadcaster.topic[IO, Int])
  checkMultipleWriters("BroadcasterTopic", () => Broadcaster.topic[IO, Int])

end BroadcasterSuite

object BroadcasterSuite:

  def runWriter(b: Broadcaster[IO, Int])(
      data: Vector[Int],
  ): IO[FiberIO[Unit]] =
    val io = data.traverse(b.send)
    io.as(()).start

  def runReader(b: Broadcaster[IO, Int])(
      intSig: Signal[IO, Boolean],
  ): IO[FiberIO[Vector[Int]]] =
    for
      s <- b.makeOut
      f <- s.interruptWhen(intSig).compile.toVector.start
    yield f

  def randData(rand: Random[IO])(num: Int): IO[Vector[Int]] =
    (1 to num).toVector.traverse(_ => rand.nextInt)

end BroadcasterSuite
