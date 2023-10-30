package ptpger.syntax

import cats.MonadThrow
import cats.data.EitherT
import io.circe.KeyDecoder
import io.circe.KeyEncoder
import io.circe._
import io.circe.parser.decodeAccumulating
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt

trait CirceSyntax {
  implicit def circeSyntaxDecoderOps(s: String): DecoderOps = new DecoderOps(s)
  implicit def circeSyntaxJsonDecoderOps(json: Json): JsonDecoderOps = new JsonDecoderOps(json)

  implicit def mapEncoder[K: Encoder, V: Encoder]: Encoder[Map[K, V]] =
    (map: Map[K, V]) => Encoder[List[(K, V)]].apply(map.toList)

  implicit def mapDecoder[K: Decoder, V: Decoder]: Decoder[Map[K, V]] =
    (c: HCursor) => c.as[List[(K, V)]].map(_.toMap)

  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] =
    Decoder[B].map(_.coerce[A])

  implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.asInstanceOf[B])

  implicit def coercibleKeyDecoder[A: Coercible[B, *], B: KeyDecoder]: KeyDecoder[A] =
    KeyDecoder[B].map(_.coerce[A])

  implicit def coercibleKeyEncoder[A: Coercible[B, *], B: KeyEncoder]: KeyEncoder[A] =
    KeyEncoder[B].contramap[A](_.asInstanceOf[B])

  implicit val passwordHashEncoder: Encoder[PasswordHash[SCrypt]] =
    Encoder.encodeString.contramap(_.toString)
  implicit val passwordHashDecoder: Decoder[PasswordHash[SCrypt]] =
    Decoder.decodeString.map(PasswordHash[SCrypt])
}

final class DecoderOps(private val s: String) {
  def decodeAs[A: Decoder]: A = decodeAccumulating[A](s).fold(
    error => throw Errors(error),
    identity,
  )
  def decodeAsF[F[_]: MonadThrow, A: Decoder]: F[A] =
    EitherT
      .fromEither[F](decodeAccumulating(s).toEither)
      .leftMap(Errors)
      .rethrowT
}
final class JsonDecoderOps(json: Json) {
  def decodeAs[A](implicit decoder: Decoder[A]): A =
    decoder
      .decodeAccumulating(json.hcursor)
      .fold(
        error => throw Errors(error),
        identity,
      )

  def decodeAsF[F[_]: MonadThrow, A](implicit decoder: Decoder[A]): F[A] =
    EitherT
      .fromEither[F](decoder.decodeAccumulating(json.hcursor).toEither)
      .leftMap(Errors)
      .rethrowT
}
