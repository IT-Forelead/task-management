package ptpger.setup

import uz.scala.aws.s3.AWSConfig
import uz.scala.flyway.MigrationsConfig
import uz.scala.http4s.HttpServerConfig
import uz.scala.redis.RedisConfig
import uz.scala.skunk.DataBaseConfig

import ptpger.auth.AuthConfig

case class Config(
    http: HttpServerConfig,
    database: DataBaseConfig,
    auth: AuthConfig,
    redis: RedisConfig,
    awsConfig: AWSConfig,
  ) {
  lazy val migrations: MigrationsConfig = MigrationsConfig(
    hostname = database.host.value,
    port = database.port.value,
    database = database.database.value,
    username = database.user.value,
    password = database.password.value,
    schema = "public",
    location = "db/migration",
  )
}
