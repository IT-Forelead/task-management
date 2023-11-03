package ptpger.routes

import cats.MonadThrow
import cats.data.OptionT
import cats.effect.kernel.Concurrent
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import org.http4s.AuthedRoutes
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe.JsonDecoder
import org.http4s.multipart.Multipart
import uz.scala.http4s.syntax.all.http4SyntaxPartOps
import uz.scala.http4s.utils.Routes

import ptpger.algebras.AssetsAlgebra
import ptpger.domain.Asset.AssetInfo
import ptpger.domain._
final case class RootRoutes[
    F[_]: JsonDecoder: MonadThrow: Concurrent: Lambda[M[_] => fs2.Compiler[M, M]]
  ](
    assets: AssetsAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/tasks"

  override val public: HttpRoutes[F] = HttpRoutes.empty[F]

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "assets" as _ =>
      ar.req.decode[Multipart[F]] { multipart =>
        for {
          assetInfo <- multipart.parts.textParts.convert[AssetInfo]
          result <- OptionT(assets.uploadFile(multipart.parts.fileParts, assetInfo.public))
            .foldF(BadRequest("File part not exists!")) { fileKey =>
              assets.create(assetInfo, fileKey).flatMap(Created(_))
            }
        } yield result
      }
  }
}
