import Dependencies.*

name := "sttp"

libraryDependencies ++= com.softwaremill.sttp.all ++ Seq(
  org.typelevel.log4cats
)
