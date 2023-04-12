package tdl.db.jdbc.scheme

import doobie.ConnectionIO
import doobie.Update0
import doobie.Update
import doobie.Query0
import doobie.LogHandler
import doobie.implicits.given
import doobie.syntax.given
import doobie.given

import tdl.model.ListId
import tdl.model.RecordId
import tdl.util.NonEmptyString

def init(using h: LogHandler = LogHandler.nop): ConnectionIO[Unit] =
  for
    _ <- createList.run
    _ <- createItem.run
  yield ()

def createList(using h: LogHandler = LogHandler.nop): Update0 =
  sql"""
    CREATE TABLE IF NOT EXISTS list (
      id varchar NOT NULL,
      name varchar NOT NULL,
      PRIMARY KEY(id)
    );
  """.update

def createItem(using h: LogHandler = LogHandler.nop): Update0 =
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

def insertList(using
    h: LogHandler = LogHandler.nop,
): Update[(ListId, NonEmptyString)] =
  val sql = """
    INSERT INTO list (id, name) VALUES(?, ?);
  """
  Update[(ListId, NonEmptyString)](sql)

def insertItem(using h: LogHandler = LogHandler.nop): Update[Item] =
  val sql = """
    INSERT INTO item (list_id, item_id, item_order, value, checked) VALUES(?, ?, ?, ?, ?);
  """
  Update[Item](sql)

def updateItem(lid: ListId, rid: RecordId, checked: Boolean)(using
    h: LogHandler = LogHandler.nop,
): Update0 =
  sql"""
    UPDATE item
    SET checked = $checked
    WHERE list_id = $lid AND item_id = $rid
  """.update

def deleteItems(using h: LogHandler = LogHandler.nop): Update[ListId] =
  val sql = """
    DELETE FROM item WHERE list_id = ?;
  """
  Update[ListId](sql)

def deleteList(using h: LogHandler = LogHandler.nop): Update[ListId] =
  val sql = """
    DELETE FROM list WHERE id = ?;
  """
  Update[ListId](sql)

def getLists(using
    h: LogHandler = LogHandler.nop,
): Query0[(ListId, NonEmptyString)] =
  sql"""
    SELECT id, name FROM list
  """.query[(ListId, NonEmptyString)]

def getListItems(lid: ListId)(using h: LogHandler = LogHandler.nop) =
  sql"""
    SELECT
      l.name, l.id, i.item_id, i.item_order, i.value, i.checked
    FROM list as l
    LEFT JOIN item as i ON i.list_id == l.id
    WHERE l.id = $lid
  """.query[(NonEmptyString, Item)]
