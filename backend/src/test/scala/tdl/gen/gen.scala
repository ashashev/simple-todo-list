package tdl.gen

import java.{util => ju}

import cats.effect.*
import cats.effect.std.Random

import tdl.model.*
import tdl.tests.munit.Ops.*
import tdl.util.NonEmptyString

def genUUID(): IO[String] =
  IO(ju.UUID.randomUUID().toString())

def genListId(): IO[ListId] =
  genUUID().map(u => ListId(s"todo-$u").value)

def genRecordId(): IO[RecordId] =
  genUUID().map(u => RecordId(s"item-$u").value)

def genNonEmptyString(length: Int): IO[NonEmptyString] =
  for
    g <- Random.scalaUtilRandom[IO]
    s <- g.nextString(length)
  yield NonEmptyString(s).value
