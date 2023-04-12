package tdl.util

import cats.Monad
import cats.Parallel
import cats.effect.Fiber
import cats.effect.GenConcurrent
import cats.effect.GenSpawn
import cats.effect.kernel.Ref
import cats.effect.std.Queue
import cats.instances.all.given
import cats.syntax.all.given

trait VarLike[F[_], A]:

  def take: F[A]

  def put(a: A): F[Unit]

end VarLike

object VarLike:

  def empty[F[_]: Monad: Parallel, A](implicit
      genConc: GenConcurrent[F, Throwable],
  ): F[VarLike[F, A]] =
    for qs <- Queue.bounded[F, A](1)
    yield new VarLike[F, A]:

      def take: F[A] = qs.take

      def put(a: A): F[Unit] = qs.offer(a)

  def of[F[_]: Monad: Parallel, A](a: A)(implicit
      genConc: GenConcurrent[F, Throwable],
  ): F[VarLike[F, A]] =
    for
      qs <- Queue.bounded[F, A](1)
      _ <- qs.offer(a)
    yield new VarLike[F, A]:

      def take: F[A] = qs.take

      def put(a: A): F[Unit] = qs.offer(a)

  extension [F[_]: Monad, A](v: VarLike[F, A])
    def update(f: A => A): F[Unit] =
      v.take.flatMap(a => v.put(f(a)))

    def updateF(f: A => F[A]): F[Unit] =
      for
        a <- v.take
        na <- f(a)
        _ <- v.put(na)
      yield ()

end VarLike
