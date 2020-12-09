import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

name := "simple-todo-list"

ThisBuild / scalaVersion := "3.0.0-M1"
ThisBuild / version := "0.2"
ThisBuild / scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  //"-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  //"-language:experimental.macros", // Allow macro definition (besides implementation and application)
  //"-language:higherKinds", // Allow higher-kinded types
  //"-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-source",
  "3.1",
  "-explain",
  "-explain-types",
  "-new-syntax"
)

lazy val root = project
  .in(file("."))
  .aggregate(work.js, work.jvm)

lazy val work = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("."))
  .settings(
    name := "simple-todo-list",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.scalatest" %% "scalatest" % "3.2.3" % Test
    ),
    libraryDependencies ++= Seq(
      "org.rudogma" %%% "supertagged" % "2.0-RC2"
    ).map(_.withDottyCompat(scalaVersion.value)),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .jvmConfigure(_.enablePlugins(JavaServerAppPackaging, SystemdPlugin))
  .jvmSettings(
    // Add JVM-specific settings here
    name := "simple-todo-list-server",
    libraryDependencies ++= {
      val akkaVersion = "2.6.10"
      val akkaHttpVersion = "10.2.1"
      Seq(
        "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
      ).map(_.withDottyCompat(scalaVersion.value))
    },
    libraryDependencies ++= Seq(
    ),
    Linux / maintainer := "Aleksei Shashev <ashashev@gmail.com>",
    maintainer := "Aleksei Shashev <ashashev@gmail.com>",
    dockerExposedPorts := Seq(8080)
  )
  .jsSettings(
    // Add JS-specific settings here
    name := "simple-todo-list-ui",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.1.0"
    ).map(_.withDottyCompat(scalaVersion.value))
  )
