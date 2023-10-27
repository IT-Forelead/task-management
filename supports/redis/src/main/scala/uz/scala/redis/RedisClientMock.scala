package uz.scala.redis

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

import cats.effect.Sync
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.toFunctorOps
import io.circe.Encoder

import uz.scala.syntax.all.genericSyntaxGenericTypeOps

object RedisClientMock {
  def apply[F[_]: Sync]: RedisClient[F] = new RedisClient[F] {
    val Redis = mutable.HashMap.empty[String, String]
    override def put(
        key: String,
        value: String,
        expire: FiniteDuration,
      ): F[Unit] = Redis.put(key, value).pure[F].void

    override def put[T: Encoder](
        key: String,
        value: T,
        expire: FiniteDuration,
      ): F[Unit] = Sync[F].delay(Redis.put(key, value.toJson)).void

    override def get(key: String): F[Option[String]] = Sync[F].delay(Redis.get(key))

    override def del(key: String*): F[Unit] = key.foreach(Redis.remove).pure[F]
  }
}
