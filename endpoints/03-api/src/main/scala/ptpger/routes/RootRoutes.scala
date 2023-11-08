package ptpger.routes

import cats.MonadThrow
import cats.data.{NonEmptyList, OptionT}
import cats.effect.kernel.Concurrent
import cats.implicits.{catsSyntaxApplicativeId, toFlatMapOps, toFunctorOps}
import io.estatico.newtype.ops._
import org.http4s.AuthedRoutes
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe.JsonDecoder
import org.http4s.headers.{`Content-Type`, `Transfer-Encoding`}
import org.http4s.multipart.Multipart
import uz.scala.http4s.syntax.all.http4SyntaxPartOps
import uz.scala.http4s.utils.Routes
import ptpger.algebras.AssetsAlgebra
import ptpger.domain.Asset.AssetInput
import ptpger.domain._
final case class RootRoutes[
    F[_]: JsonDecoder: MonadThrow: Concurrent: Lambda[M[_] => fs2.Compiler[M, M]]
  ](
    assets: AssetsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  private val AllowedMediaTypes: List[MediaType] = List(
    MediaType.unsafeParse("application/pdf"),
    MediaType.unsafeParse(
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ),
    MediaType.unsafeParse("image/png"),
    MediaType.unsafeParse("image/jpeg"),
    MediaType.unsafeParse("application/msword"),
    MediaType.unsafeParse("application/wps-office.doc"),
    MediaType.unsafeParse("application/wps-office.docx"),
  )
  override val path = "/"

  override val public: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "assets" / "view" / UUIDVar(id) =>
      for {
        obj <- assets.downloadObject(id.coerce[AssetId])
      } yield Response(
        body = obj,
        headers = Headers(
          `Content-Type`(MediaType.application.`octet-stream`),
          `Transfer-Encoding`(TransferCoding.chunked.pure[NonEmptyList]),
        ),
      )
  }

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "assets" as _ =>
      ar.req.decode[Multipart[F]] { multipart =>
        for {
          assetInfo <- multipart.parts.convert[AssetInput]
          result <- OptionT(
            assets.uploadFile(multipart.parts.fileParts(AllowedMediaTypes: _*), assetInfo.public)
          )
            .foldF(BadRequest("File part not exists!")) { fileKey =>
              assets.create(assetInfo, fileKey).flatMap(Created(_))
            }
        } yield result
      }
    case GET -> Root / "assets" / UUIDVar(id) as _ =>
      assets.getPublicUrl(id.coerce[AssetId]).flatMap(Ok(_))
  }
}
