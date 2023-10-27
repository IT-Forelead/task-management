name := "redis"

libraryDependencies ++= Dependencies.dev.profunktor.redis4cats.all

dependsOn(LocalProject("common"))
