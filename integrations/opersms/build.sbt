import Dependencies.*

name         := "opersms"
scalaVersion := "2.13.10"

libraryDependencies ++=
  Seq(
    Dependencies.Testing.`weaver-cats`,
    Dependencies.Testing.`weaver-scala-check`,
    Dependencies.Testing.`weaver-discipline`,
    Dependencies.Testing.`refined-scalacheck`,
  ).map(_ % Test)

dependsOn(
  LocalProject("common"),
  LocalProject("support_sttp"),
)
