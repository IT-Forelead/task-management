package uz.scala.redis

import scala.concurrent.duration.FiniteDuration

import cats._
import cats.implicits.toFunctorOps
import dev.profunktor.redis4cats.RedisCommands
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Encoder

import uz.scala.syntax.all.genericSyntaxGenericTypeOps

trait RedisClient[F[_]] {
  def put(
      key: String,
      value: String,
      expire: FiniteDuration,
    ): F[Unit]

  def put[A: Encoder](
      key: String,
      value: A,
      expire: FiniteDuration,
    ): F[Unit]

  def get(key: String): F[Option[String]]

  def del(key: String*): F[Unit]
}

object RedisClient {
  def apply[F[_]: MonadThrow](
      redis: RedisCommands[F, String, String],
      prefix: NonEmptyString,
    ): RedisClient[F] =
    new RedisClient[F] {
      override def put(
          key: String,
          value: String,
          expire: FiniteDuration,
        ): F[Unit] = redis.setEx(s"$prefix:$key", value, expire)

      override def put[A: Encoder](
          key: String,
          value: A,
          expire: FiniteDuration,
        ): F[Unit] =
        redis.setEx(s"$prefix:$key", value.toJson, expire)

      override def get(key: String): F[Option[String]] = redis.get(s"$prefix:$key")

      override def del(key: String*): F[Unit] = redis.del(key.map(k => s"$prefix:$k"): _*).void
    }
}
