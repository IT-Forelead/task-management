package uz.scala.flyway

import java.sql.DriverManager

import cats.effect.Sync
import cats.syntax.all._
import org.typelevel.log4cats.Logger

trait Migrations[F[_]] {
  /** Runs a migration */
  def migrate: F[Unit]

  /** Validates migrations. Throws in case of validation errors */
  def validate: F[Unit]
}

object Migrations {
  /** Synchronously runs migrations */
  def run[F[_]: Sync: Logger](
      config: MigrationsConfig
    ): F[Unit] =
    make[F](config).flatMap(_.migrate)

  def make[F[_]: Sync](
      config: MigrationsConfig
    )(implicit
      logger: Logger[F]
    ): F[Migrations[F]] =
    for {
      _ <- createSchema[F](config)
      _ <- logger.info(s"Created schema if it didnt exist: ${config.schema}")
      flyway <- Sync[F].delay(Flyway.unsafeConfigure(config))
    } yield new Implementation[F](flyway)

  private def createSchema[F[_]: Sync](
      config: MigrationsConfig
    ): F[Unit] =
    Sync[F].blocking {
      val conn = DriverManager.getConnection(config.rootUrl)
      val stmt = conn.createStatement()
      stmt.execute(s"CREATE SCHEMA IF NOT EXISTS ${config.schema}")
      stmt.closeOnCompletion()
    }

  final private[this] class Implementation[F[_]: Sync](
      flyway: org.flywaydb.core.Flyway
    )(implicit
      logger: Logger[F]
    ) extends Migrations[F] {
    def migrate: F[Unit] =
      Sync[F]
        .blocking(flyway.migrate())
        .void
        .onError(err => logger.error(err)("Migration run error"))

    def validate: F[Unit] =
      Sync[F].blocking(flyway.validate())
  }
}
