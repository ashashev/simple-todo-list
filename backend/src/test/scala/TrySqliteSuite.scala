import cats.effect.*
import cats.effect.kernel.Outcome.Succeeded
import cats.effect.std.Random
import cats.effect.std.Random.apply
import cats.instances.all.given
import cats.syntax.all.given
import doobie.Transactor
import doobie.ConnectionIO
import doobie.given
import doobie.implicits.given
import doobie.munit.IOChecker
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import munit.CatsEffectSuite
import doobie.util.update.Update

class TrySqliteSuite extends CatsEffectSuite with IOChecker:

  import TrySqliteSuite.*

  override val colors = doobie.util.Colors.None

  val transactor =
    Transactor.fromDriverManager[IO]("org.sqlite.JDBC", "jdbc:sqlite:sample.db")

  val init =
    (for
      cd <- drop.run
      cc <- create.run
      ci <- insert
      _ <- Sync[ConnectionIO].delay(println(s"$cd, $cc, $ci"))
    yield ()).transact(transactor)

  init.unsafeRunSync()

  test("get bigger") {
    for
      rs <- biggerThan(160).to[List].transact(transactor)
      _ <- IO(
        assertEquals(
          rs,
          Country("CN3", "Greate Country 3", 170, BigDecimal(370.570).some) ::
            Country("CN4", "Greate Country 4", 270, BigDecimal(470.350).some) ::
            Nil,
        ),
      )
    yield ()
  }

  // test("trivial") { check(trivial) }
  // test("biggerThan") { check(biggerThan(0)) }
  test("update") { check(update("", "")) }
  test("biggerThan") { checkOutput(biggerThan(0)) }

end TrySqliteSuite

object TrySqliteSuite:

  case class Country(
      code: String,
      name: String,
      pop: Int,
      gnp: Option[BigDecimal],
  )
  case class FullCountry(
      code: String,
      name: String,
      pop: Int,
      gnp: Option[Double],
      indepyear: Option[Short],
  )

  val drop =
    sql"""
      DROP TABLE IF EXISTS country;
    """.update

  val create =
    sql"""
      CREATE TABLE IF NOT EXISTS country (
        code        character(3)  NOT NULL,
        name        text          NOT NULL,
        population  integer       NOT NULL,
        gnp         numeric(10,2),
        indepyear   smallint
        -- more columns, but we won't use them here
      );
    """.update

  val countries =
    FullCountry(
      "CN1",
      "Greate Country 1",
      150,
      300.500.some,
      (1000: Short).some,
    ) ::
      FullCountry(
        "CN2",
        "Greate Country 2",
        140,
        270.575.some,
        (1000: Short).some,
      ) ::
      FullCountry(
        "CN3",
        "Greate Country 3",
        170,
        370.570.some,
        (1000: Short).some,
      ) ::
      FullCountry(
        "CN4",
        "Greate Country 4",
        270,
        470.350.some,
        (1000: Short).some,
      ) ::
      Nil

  val insert =
    val sql = """
      INSERT INTO country(code, name, population, gnp, indepyear) VALUES(?, ?, ?, ?, ?);
    """
    Update[FullCountry](sql).updateMany(countries)

  val trivial =
    sql"""
      select 42, 'foo'
    """.query[(Int, String)]

  def biggerThan(minPop: Int) =
    sql"""
      select code, name, population, gnp
      from country
      where population > $minPop
    """.query[Country]

  def update(oldName: String, newName: String) =
    sql"""
      update country set name = $newName where name = $oldName
    """.update

end TrySqliteSuite
