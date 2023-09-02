package tdl.db.jdbc

import cats.effect.MonadCancelThrow
import cats.implicits.given
import doobie.Transactor
import doobie.implicits.given

import tdl.model
import tdl.model.ListId
import tdl.model.RecordId
import tdl.util.NonEmptyString

class TodoStore[F[_]: MonadCancelThrow](tr: Transactor[F])
    extends tdl.model.TodoStore[F]:

  def getLists(): F[List[(ListId, NonEmptyString)]] =
    scheme.getLists.to[List].transact(tr)

  def update(id: ListId, rid: RecordId, checked: Boolean): F[Unit] =
    scheme.updateItem(id, rid, checked).run.transact(tr).as(())

  def get(id: ListId): F[Option[(NonEmptyString, List[model.Record])]] =
    scheme
      .getListItems(id)
      .to[List]
      .map {
        case Nil => None
        case xs =>
          val sorted = xs.sortBy(_._2.order)
          val name = sorted.head._1
          val rs = sorted.map {
            case (_, scheme.Item(_, rid, _, value, checked)) =>
              model.Record(rid, value, checked)
          }
          Some(name -> rs)
      }
      .transact(tr)

  def replace(
      id: ListId,
      name: NonEmptyString,
      rs: List[model.Record],
  ): F[Unit] =
    val items = rs.zipWithIndex.map { case (r, i) =>
      scheme.Item(id, r.id, i, r.value, r.checked)
    }

    val upd =
      for
        _ <- scheme.deleteItems.toUpdate0(id).run
        _ <- scheme.deleteList.toUpdate0(id).run
        _ <- scheme.insertList.toUpdate0(id -> name).run
        _ <- scheme.insertItem.updateMany(items)
      yield ()

    upd.transact(tr)

end TodoStore

object TodoStore:
  def apply[F[_]: MonadCancelThrow](tr: Transactor[F]): TodoStore[F] =
    new TodoStore[F](tr)
end TodoStore
