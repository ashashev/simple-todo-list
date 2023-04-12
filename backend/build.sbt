val scala3Version = "3.2.2"

val Http4sVersion = "1.0.0-M39"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.4.6"
val MunitCatsEffectVersion = "1.0.7"
val Fs2Version = "3.6.1"
val CirceVersion = "0.14.3"
val DoobieVersion = "1.0.0-RC2"
val SqliteJdbcVersion = "3.41.2.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "Backend",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-ember-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.scalameta" %% "munit" % MunitVersion % Test,
      "org.typelevel" %% "munit-cats-effect-3" % MunitCatsEffectVersion % Test,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "co.fs2" %% "fs2-core" % Fs2Version,
      "co.fs2" %% "fs2-io" % Fs2Version,
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,

      "org.xerial" % "sqlite-jdbc" % SqliteJdbcVersion,
      "org.tpolecat" %% "doobie-core" % DoobieVersion,
      //"org.tpolecat" %% "doobie-h2" % DoobieVersion, // H2 driver 1.4.200 + type mappings.
      //"org.tpolecat" %% "doobie-hikari" % DoobieVersion, // HikariCP transactor.
      //"org.tpolecat" %% "doobie-postgres" % DoobieVersion, // Postgres driver 42.3.1 + type mappings.
      "org.tpolecat" %% "doobie-munit" % DoobieVersion % "test", // Specs2 support for typechecking statements.
    ),
    scalacOptions ++= Seq(
      "-feature", // then put the next option on a new line for easy editing
      // "-language:implicitConversions",
      "-language:existentials",
      "-unchecked",
      "-Werror",
      // "-java-output-version", "18",
      "-new-syntax",
      "-indent",
      // "-rewrite",
      "-explain",
    ),
    fork := true,
  )
