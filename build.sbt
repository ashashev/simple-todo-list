name := "simple-todo-list"

scalaVersion in ThisBuild := "2.12.4"

lazy val akkaHttpVersion = "10.0.11"
lazy val akkaVersion = "2.4.20" //version akka for akka http
lazy val scalaTestVersion = "3.0.1"

lazy val root = project.in(file(".")).
  aggregate(backend, frontend).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val work = crossProject.in(file(".")).
  settings(
    name := "simple-todo-list",
    version := "0.1",
    scalacOptions ++= Seq("-deprecation", "-feature", "-encoding", "utf8", "-Ywarn-dead-code", "-unchecked", "-Xlint", "-Ywarn-unused-import"),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
    )
  ).
  jvmSettings(
    // Add JVM-specific settings here
    name := "simple-todo-list-server",
    coverageEnabled := true,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
      "commons-daemon" % "commons-daemon" % "1.0.15"
    )
  ).
  jsSettings(
    // Add JS-specific settings here
    name := "simple-todo-list-ui",
    scalaJSUseMainModuleInitializer := true,

    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.1"
    )
  )

lazy val backend = work.jvm
lazy val frontend = work.js
