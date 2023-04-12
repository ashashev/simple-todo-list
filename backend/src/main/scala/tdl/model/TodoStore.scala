package tdl.model

import scala.collection.concurrent.TrieMap

import cats.Monad
import cats.instances.all.given
import cats.syntax.all.given

import tdl.util.NonEmptyString

trait TodoStore[F[_]]:
  def get(id: ListId): F[Option[(NonEmptyString, List[Record])]]
  def replace(id: ListId, name: NonEmptyString, rs: List[Record]): F[Unit]
  def update(id: ListId, rid: RecordId, checked: Boolean): F[Unit]
  def getLists(): F[List[(ListId, NonEmptyString)]]
end TodoStore

object TodoStore:

  def memEmpty[F[_]: Monad]: TodoStore[F] =
    new TodoStore[F]:
      private val store = TrieMap.empty[ListId, (NonEmptyString, List[Record])]

      def get(id: ListId): F[Option[(NonEmptyString, List[Record])]] =
        Monad[F].unit.as(store.get(id))

      def replace(id: ListId, name: NonEmptyString, rs: List[Record]): F[Unit] =
        Monad[F].unit.as(store.update(id, (name, rs)))

      def update(id: ListId, rid: RecordId, checked: Boolean): F[Unit] =
        Monad[F].unit.as(store.updateWith(id) {
          case None =>
            None
          case Some((name, rs)) =>
            (name -> rs.map {
              case Record(`rid`, value, _) =>
                Record(rid, value, checked)
              case r => r
            }).some
        })

      def getLists(): F[List[(ListId, NonEmptyString)]] =
        Monad[F].unit.as(store.toList.map { case (lid, (name, _)) =>
          (lid, name)
        })

end TodoStore
