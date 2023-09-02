package tdl.db.jdbc.scheme

import doobie.ConnectionIO
import doobie.Query0
import doobie.Update
import doobie.Update0
import doobie.implicits.given

import tdl.model.ListId
import tdl.model.RecordId
import tdl.util.NonEmptyString

def init: ConnectionIO[Unit] =
  for
    _ <- createList.run
    _ <- createItem.run
  yield ()

def createList: Update0 =
  sql"""
    CREATE TABLE IF NOT EXISTS list (
      id varchar NOT NULL,
      name varchar NOT NULL,
      PRIMARY KEY(id)
    );
  """.update

def createItem: Update0 =
  sql"""
    CREATE TABLE IF NOT EXISTS item (
      list_id varchar NOT NULL,
      item_id varchar NOT NULL,
      item_order integer NOT NULL,
      value varchar NOT NULL,
      checked boolean NOT NULL,
      PRIMARY KEY(list_id, item_id)
    );
  """.update

def insertList: Update[(ListId, NonEmptyString)] =
  val sql = """
    INSERT INTO list (id, name) VALUES(?, ?);
  """
  Update[(ListId, NonEmptyString)](sql)

def insertItem: Update[Item] =
  val sql = """
    INSERT INTO item (list_id, item_id, item_order, value, checked) VALUES(?, ?, ?, ?, ?);
  """
  Update[Item](sql)

def updateItem(lid: ListId, rid: RecordId, checked: Boolean): Update0 =
  sql"""
    UPDATE item
    SET checked = $checked
    WHERE list_id = $lid AND item_id = $rid
  """.update

def deleteItems: Update[ListId] =
  val sql = """
    DELETE FROM item WHERE list_id = ?;
  """
  Update[ListId](sql)

def deleteList: Update[ListId] =
  val sql = """
    DELETE FROM list WHERE id = ?;
  """
  Update[ListId](sql)

def getLists: Query0[(ListId, NonEmptyString)] =
  sql"""
    SELECT id, name FROM list
  """.query[(ListId, NonEmptyString)]

def getListItems(lid: ListId): Query0[(NonEmptyString, Item)] =
  sql"""
    SELECT
      l.name, l.id, i.item_id, i.item_order, i.value, i.checked
    FROM list as l
    LEFT JOIN item as i ON i.list_id == l.id
    WHERE l.id = $lid
  """.query[(NonEmptyString, Item)]
