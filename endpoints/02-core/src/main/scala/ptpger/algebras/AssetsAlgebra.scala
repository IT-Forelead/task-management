package ptpger.algebras

import java.net.URL

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.multipart.Part
import uz.scala.aws.s3.S3Client
import uz.scala.syntax.refined.commonSyntaxAutoRefineOptV

import ptpger.domain.Asset
import ptpger.domain.Asset.AssetInfo
import ptpger.domain.Asset.AssetInput
import ptpger.domain.AssetId
import ptpger.effects.Calendar
import ptpger.effects.GenUUID
import ptpger.exception.AError
import ptpger.repos.AssetsRepository
import ptpger.utils.ID

trait AssetsAlgebra[F[_]] {
  def create(assetInfo: AssetInput, fileKey: NonEmptyString): F[AssetId]
  def uploadFile(
      parts: Vector[Part[F]],
      public: Boolean,
    ): F[Option[NonEmptyString]]
  def getPublicUrl(assetIds: NonEmptyList[AssetId]): F[Map[AssetId, URL]]
  def getPublicUrl(assetId: AssetId): F[AssetInfo]
}
object AssetsAlgebra {
  def make[F[_]: MonadThrow: GenUUID: Calendar: Lambda[M[_] => fs2.Compiler[M, M]]](
      assetsRepository: AssetsRepository[F],
      s3Client: S3Client[F],
    ): AssetsAlgebra[F] =
    new AssetsAlgebra[F] {
      override def create(assetInfo: AssetInput, fileKey: NonEmptyString): F[AssetId] =
        for {
          id <- ID.make[F, AssetId]
          now <- Calendar[F].currentZonedDateTime
          asset = Asset(
            id = id,
            createdAt = now,
            s3Key = fileKey,
            public = assetInfo.public,
            fileName = assetInfo.filename,
          )
          _ <- assetsRepository.create(asset)
        } yield id

      override def uploadFile(
          parts: Vector[Part[F]],
          public: Boolean,
        ): F[Option[NonEmptyString]] = {
        val files = parts.flatMap(p => p.filename.map(_ -> p.body).toVector)
        files
          .traverse {
            case (filename, body) =>
              body.through(uploadToS3(filename, public))
          }
          .compile
          .toVector
          .map(_.flatten.headOption)
      }

      override def getPublicUrl(assetIds: NonEmptyList[AssetId]): F[Map[AssetId, URL]] =
        for {
          assets <- assetsRepository.getAssets(assetIds)
          assetUrls <- assets.toList.traverse {
            case assetId -> asset =>
              s3Client.objectUrl(asset.s3Key.value).map(assetId -> _)
          }
        } yield assetUrls.toMap
      override def getPublicUrl(assetId: AssetId): F[AssetInfo] =
        OptionT(assetsRepository.findAsset(assetId)).foldF(
          AError.Internal(s"File not found by assetId [$assetId]").raiseError[F, AssetInfo]
        ) { asset =>
          s3Client.objectUrl(asset.s3Key.value).map { url =>
            AssetInfo(asset.public, asset.fileName, url)
          }
        }

      private def getFileType(filename: String): String = filename.dropWhile(_ == '.').tail

      private def genFileKey(orgFilename: String): F[String] =
        GenUUID[F].make.map { uuid =>
          uuid.toString + "." + getFileType(orgFilename)
        }

      private def uploadToS3(filename: String, public: Boolean): fs2.Pipe[F, Byte, String] = body =>
        for {
          key <- fs2.Stream.eval(genFileKey(filename))
          _ <- body.through(s3Client.putObject(key, public))
        } yield key
    }
}
