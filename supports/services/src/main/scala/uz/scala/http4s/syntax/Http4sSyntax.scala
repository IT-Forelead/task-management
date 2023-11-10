package uz.scala.http4s.syntax

import java.time.ZonedDateTime

import scala.concurrent.duration.DurationInt

import cats.MonadThrow
import cats.effect.Temporal
import cats.effect.kernel.Concurrent
import cats.implicits._
import eu.timepit.refined.types.string.NonEmptyString
import fs2.RaiseThrowable
import io.circe.Decoder
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.headers.`Content-Type`
import org.http4s.multipart.Part
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame.Ping
import org.http4s.websocket.WebSocketFrame.Text
import org.typelevel.log4cats.Logger
import uz.scala.MultipartDecodeError
import uz.scala.http4s.utils.MapConvert
import uz.scala.http4s.utils.MapConvert.ValidationResult
import uz.scala.syntax.all.circeSyntaxDecoderOps
import uz.scala.syntax.all.genericSyntaxGenericTypeOps

trait Http4sSyntax {
  implicit def http4SyntaxReqOps[F[_]: JsonDecoder: MonadThrow](
      request: Request[F]
    ): RequestOps[F] =
    new RequestOps(request)
  implicit def http4SyntaxPartOps[F[_]](parts: Vector[Part[F]]): PartOps[F] =
    new PartOps(parts)

  implicit def http4SyntaxGenericTypeOps[A](obj: A): GenericTypeOps[A] =
    new GenericTypeOps[A](obj)

  implicit def deriveEntityEncoder[F[_], A: Encoder]: EntityEncoder[F, A] =
    jsonEncoderOf[F, A]

  implicit def deriveEntityDecoder[F[_]: Concurrent, A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]

  implicit val zonedDateTimeQueryParamDecoder: QueryParamDecoder[ZonedDateTime] =
    QueryParamDecoder[String].map(ZonedDateTime.parse)

  implicit def http4SyntaxWSOps[F[_]: Temporal: Logger](wsb: WebSocketBuilder2[F]): WSOps[F] =
    new WSOps(wsb)
}

final class WSOps[F[_]](wsb: WebSocketBuilder2[F])(implicit F: Temporal[F], logger: Logger[F]) {
  def createChannel[A: Decoder: Encoder](
      stream: fs2.Stream[F, A],
      handle: A => F[Unit],
      onClose: F[Unit] = F.unit,
    ): F[Response[F]] =
    wsb
      .withOnClose(onClose >> logger.info(s"Websocket closed"))
      .build(
        fs2
          .Stream(
            fs2.Stream.awakeDelay(30.seconds).as(Ping()),
            stream
              .map { msg =>
                Text(msg.toJson)
              },
          )
          .parJoinUnbounded,
        _.evalMap {
          case Text(data, _) =>
            data.decodeAsF[F, A].flatMap(handle)
        },
      )
}
final class RequestOps[F[_]: JsonDecoder: MonadThrow](private val request: Request[F])
    extends Http4sDsl[F] {
  def decodeR[A: Decoder](
      handle: A => F[Response[F]]
    )(implicit
      decoder: Decoder[A]
    ): F[Response[F]] =
    request
      .asJson
      .map(json => decoder.decodeAccumulating(json.hcursor))
      .flatMap(
        _.fold(
          error => UnprocessableEntity(error.toList.map(_.getMessage).mkString("\n| ")),
          handle,
        )
      )

  def bearer(token: NonEmptyString): Request[F] =
    request.putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, token.value)))
}

final class PartOps[F[_]](private val parts: Vector[Part[F]]) {
  private def filterFileTypes(part: Part[F]): Boolean = part.filename.exists(_.trim.nonEmpty)
  def fileParts: Vector[Part[F]] = parts.filter(filterFileTypes)
  def fileParts(mediaTypes: MediaType*): Vector[Part[F]] =
    parts.filter(_.headers.get[`Content-Type`].exists(h => mediaTypes.contains(h.mediaType)))

  private def textParts: Vector[Part[F]] = parts.filter(_.name.exists(_.trim.nonEmpty))

  def convert[A](
      implicit
      F: MonadThrow[F],
      mapper: MapConvert[ValidationResult[A]],
      compiler: fs2.Compiler[F, F],
      RT: RaiseThrowable[F],
    ): F[A] =
    for {
      collectKV <- textParts.traverse { part =>
        part.bodyText.compile.foldMonoid.map(part.name.get -> _)
      }
      entity = mapper.fromMap(collectKV.toMap)
      validEntity <- entity.fold(
        error => F.raiseError[A](MultipartDecodeError(error.toList.mkString("\n"))),
        success => success.pure[F],
      )
    } yield validEntity
}

final class GenericTypeOps[A](obj: A) {
  def toFormData[F[_]](implicit encoder: Encoder.AsObject[A]): Vector[Part[F]] =
    obj
      .asJsonObject
      .toVector
      .map {
        case k -> v =>
          k -> v.asString
      }
      .collect {
        case k -> Some(v) =>
          Part.formData[F](k, v)
      }
}
