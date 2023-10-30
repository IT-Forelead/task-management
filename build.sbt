import Dependencies.*

ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "2.13.12"

lazy val `task-management` =
  project
    .in(file("."))
    .settings(
      name := "task-management"
    )
    .aggregate(
      endpoints,
      integrations,
      supports,
      common,
    )

lazy val common =
  project
    .in(file("common"))
    .settings(
      name := "common",
      libraryDependencies ++=
        Dependencies.io.circe.all ++
          Dependencies.Cats.all ++
          Dependencies.Enumeratum.all ++
          Dependencies.Ciris.all ++
          Dependencies.Circe.all ++
          eu.timepit.refined.all ++
          com.github.pureconfig.all ++
          com.beachape.enumeratum.all ++
          tf.tofu.derevo.all ++
          Seq(
            uz.scala.common,
            org.typelevel.cats.core,
            org.typelevel.cats.effect,
            org.typelevel.log4cats,
            ch.qos.logback,
            dev.optics.monocle,
            Dependencies.io.estatico.newtype,
            Dependencies.io.github.jmcardon.`tsec-password`,
          ),
    )
    .dependsOn(LocalProject("support_logback"))

lazy val integrations = project
  .in(file("integrations"))
  .settings(
    name := "integrations"
  )

lazy val supports = project
  .in(file("supports"))
  .settings(
    name := "supports"
  )

lazy val endpoints = project
  .in(file("endpoints"))
  .settings(
    name := "endpoints"
  )

addCommandAlias(
  "styleCheck",
  "all scalafmtSbtCheck; scalafmtCheckAll; Test / compile; scalafixAll --check",
)

Global / lintUnusedKeysOnLoad := false
Global / onChangedBuildSource := ReloadOnSourceChanges
