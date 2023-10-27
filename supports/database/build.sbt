import Dependencies.*

name := "database"

dependsOn(LocalProject("common"))
libraryDependencies ++=
  Seq(
    org.flywaydb.core,
    org.postgresql,
  )
