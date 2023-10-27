package uz.scala.flyway

import org.flywaydb.core.{ Flyway => Fway }

private[flyway] object Flyway {
  def unsafeConfigure(config: MigrationsConfig): Fway =
    Fway
      .configure()
      .dataSource(config.url, config.username, config.password)
      .locations(config.location)
      .schemas(config.schema)
      .baselineOnMigrate(true)
      .table("schema_history")
      .load()
}
