import Dependencies.*

name := "services"

libraryDependencies ++= org.http4s.all
dependsOn(LocalProject("common"))
