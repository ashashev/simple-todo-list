package tdl

import cats.Monad
import cats.Parallel
import cats.effect.Fiber
import cats.effect.GenConcurrent
import cats.effect.GenSpawn
import cats.effect.kernel.Ref
import cats.effect.std.Mutex
import cats.effect.std.Queue
import cats.instances.all.given
import cats.syntax.all.given
import fs2.concurrent.Topic

import tdl.util.VarLike

trait Broadcaster[F[_], A]:
  def send(a: A): F[Unit]
  def makeOut: F[fs2.Stream[F, A]]
end Broadcaster

object Broadcaster:

  def simple[F[_]: Monad: Parallel, A](using
      genConc: GenConcurrent[F, Throwable],
  ): F[Broadcaster[F, A]] =
    for qqs <- VarLike.of[F, Set[Queue[F, A]]](Set.empty)
    yield new BroadcasterSimple(qqs)

  def topic[F[_]: Monad: Parallel, A](using
      genConc: GenConcurrent[F, Throwable],
  ): F[BroadcasterTopic[F, A]] =
    for
      t <- Topic[F, A]
      m <- Mutex[F]
    yield new BroadcasterTopic(t, m)

end Broadcaster

class BroadcasterSimple[F[_]: Monad: Parallel, A](
    qqs: VarLike[F, Set[Queue[F, A]]],
)(using
    genConc: GenConcurrent[F, Throwable],
) extends Broadcaster[F, A]:

  def send(a: A): F[Unit] =
    for
      qs <- qqs.take
      _ <- qs.toSeq.parTraverse(q => q.offer(a)).as(())
      _ <- qqs.put(qs)
    yield ()

  def makeOut: F[fs2.Stream[F, A]] =
    for
      q <- Queue.unbounded[F, A]
      qs <- qqs.take
      _ <- qqs.put(qs + q)
    yield fs2.Stream
      .fromQueueUnterminated(q, 5)
      .onFinalize(qqs.update(_ - q))

end BroadcasterSimple

class BroadcasterTopic[F[_]: Monad: Parallel, A](
    topic: Topic[F, A],
    mutex: Mutex[F],
)(using
    genConc: GenConcurrent[F, Throwable],
) extends Broadcaster[F, A]:

  def send(a: A): F[Unit] =
    mutex.lock.surround(topic.publish1(a).as(()))

  def makeOut: F[fs2.Stream[F, A]] =
    Monad[F].unit.as(topic.subscribe(5))

end BroadcasterTopic
