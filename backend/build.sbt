val scala3Version = "3.3.0"

val Http4sVersion = "1.0.0-M39"
val MunitVersion = "0.7.29"
//val MunitVersion = "1.0.0-M8"
val LogbackVersion = "1.4.11"
val MunitCatsEffectVersion = "1.0.7"
val Fs2Version = "3.9.0"
val CirceVersion = "0.14.6"
val DoobieVersion = "1.0.0-RC4"
val SqliteJdbcVersion = "3.43.0.0"
val Log4CatsSlf4jVersion = "2.6.0"

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
      "org.typelevel" %% "log4cats-slf4j" % Log4CatsSlf4jVersion,
      "co.fs2" %% "fs2-core" % Fs2Version,
      "co.fs2" %% "fs2-io" % Fs2Version,
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "org.xerial" % "sqlite-jdbc" % SqliteJdbcVersion,
      "org.tpolecat" %% "doobie-core" % DoobieVersion,
      // "org.tpolecat" %% "doobie-h2" % DoobieVersion, // H2 driver 1.4.200 + type mappings.
      // "org.tpolecat" %% "doobie-hikari" % DoobieVersion, // HikariCP transactor.
      // "org.tpolecat" %% "doobie-postgres" % DoobieVersion, // Postgres driver 42.3.1 + type mappings.
      "org.tpolecat" %% "doobie-munit" % DoobieVersion % "test", // Specs2 support for typechecking statements.
    ),
    scalacOptions ++= Seq(
      "-deprecation", // Emit warning and location for usages of deprecated APIs.
      "-feature", // Emit warning and location for usages of features that should be imported explicitly.
      //  "-language:implicitConversions",   // Allow definition of implicit functions called views. Disabled, as it might be dropped in Scala 3. Instead use extension methods (implemented as implicit class Wrapper(val inner: Foo) extends AnyVal {}
      // "-java-output-version", "18",
      "-new-syntax",
      "-indent",
      // "-rewrite",
      "-explain",
      "-explain-types", // Explain type errors in more detail.
      "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
      //  "-language:experimental.macros",   // Allow macro definition (besides implementation and application). Disabled, as this will significantly change in Scala 3
      "-language:higherKinds", // Allow higher-kinded types
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
      // "-Werror", // Fail the compilation if there are any warnings.

      "-Wunused:imports", // Warn if an import selector is not referenced.
      "-Wunused:privates", // Warn if a private member is unused.
      "-Wunused:locals", // Warn if a local definition is unused.
      "-Wunused:explicits", // Warn if an explicit parameter is unused.
      "-Wunused:implicits", // Warn if an implicit parameter is unused.
      "-Wunused:params", // Enable -Wunused:explicits,implicits.
      "-Wunused:linted",
      "-Wvalue-discard", // Warn when non-Unit expression results are unused.
    ),
    fork := true,
  )
