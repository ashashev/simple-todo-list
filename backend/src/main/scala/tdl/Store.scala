package tdl

import cats.Monad
import cats.effect.MonadCancel
import cats.effect.Ref
import cats.Parallel
import cats.effect.Fiber
import cats.effect.Concurrent
import cats.effect.GenConcurrent
import cats.effect.std.Mutex
import cats.syntax.all.given
import fs2.concurrent.Topic

import tdl.model.*
import tdl.util.*
import tdl.api.WsEvent
import tdl.api.ListUpdated
import tdl.api.ItemUpdated

class Store[F[_]: Parallel](
    store: TodoStore[F],
    mutex: Mutex[F],
    topic: Topic[F, WsEvent],
)(using
    mc: MonadCancel[F, Throwable],
) extends TodoStore[F]:

  def subscribe(id: ListId): fs2.Stream[F, WsEvent] =
    fs2.Stream.resource {
      topic.subscribeAwait(5).map { evs =>
        fs2.Stream(()).evalMapFilter { _ =>
          get(id).map(_.map { case (name, rs) => ListUpdated(id, name, rs) })
        } ++ evs.filter(_.lid == id)
      }
    }.flatten

  def get(id: ListId): F[Option[(NonEmptyString, List[Record])]] =
    mutex.lock.surround {
      store.get(id)
    }

  def replace(id: ListId, name: NonEmptyString, rs: List[Record]): F[Unit] =
    mutex.lock.surround {
      for
        _ <- store.replace(id, name, rs)
        _ <- topic.publish1(ListUpdated(id, name, rs))
      yield ()
    }

  def update(id: ListId, rid: RecordId, checked: Boolean): F[Unit] =
    mutex.lock.surround {
      for
        _ <- store.update(id, rid, checked)
        _ <- topic.publish1(ItemUpdated(id, rid, checked))
      yield ()
    }

  def getLists(): F[List[(ListId, NonEmptyString)]] =
    mutex.lock.surround {
      store.getLists()
    }

end Store

object Store:

  def apply[F[_]: Monad: Parallel: Concurrent](store: TodoStore[F])(using
      mc: MonadCancel[F, Throwable],
  ): F[Store[F]] =
    for
      mutex <- Mutex[F]
      topic <- Topic[F, WsEvent]
    yield new Store(store, mutex, topic)

end Store
