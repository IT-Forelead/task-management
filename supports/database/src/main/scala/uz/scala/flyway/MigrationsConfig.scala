package uz.scala.flyway

case class MigrationsConfig(
    hostname: String,
    port: Int,
    database: String,
    username: String,
    password: String,
    schema: String,
    location: String,
  ) {
  lazy val rootUrl: String =
    s"jdbc:postgresql://$hostname:$port/$database?user=$username&password=$password"

  lazy val url: String =
    s"jdbc:postgresql://$hostname:$port/$database"
}
