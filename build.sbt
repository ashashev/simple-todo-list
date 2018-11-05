import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

name := "simple-todo-list"

ThisBuild / scalaVersion := "2.12.4"

lazy val akkaHttpVersion = "10.1.5"
lazy val akkaVersion = "2.5.18"
lazy val akkaStreamVersion = akkaVersion
lazy val scalaTestVersion = "3.0.5"

lazy val work = crossProject(JSPlatform, JVMPlatform).
  crossType(CrossType.Full).in(file(".")).
  settings(
    name := "simple-todo-list",
    version := "0.1",
    scalacOptions ++= Seq("-deprecation", "-feature", "-encoding", "utf8",
      "-Ywarn-dead-code", "-unchecked", "-Xlint", "-Ywarn-unused-import"),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    )
  ).
  jvmSettings(
    // Add JVM-specific settings here
    name := "simple-todo-list-server",
    coverageEnabled := true,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaStreamVersion % Test,
      "commons-daemon" % "commons-daemon" % "1.0.15"
    )
  ).
  jsSettings(
    // Add JS-specific settings here
    name := "simple-todo-list-ui",
    scalaJSUseMainModuleInitializer := true,

    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.6"
    )
  )
