package ptpger.algebras

import java.net.URL

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.catsSyntaxApplicativeId
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.headers.`Content-Type`
import org.http4s.multipart.Part
import uz.scala.aws.s3.S3Client
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

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
  def create(
      assetInfo: AssetInput,
      fileKey: String,
      mediaType: String,
    ): F[AssetId]

  def uploadFile(
      parts: Vector[Part[F]],
      public: Boolean,
    ): F[Option[(String, String)]]
  def getPublicUrl(assetIds: NonEmptyList[AssetId]): F[Map[AssetId, URL]]
  def getPublicUrl(assetId: AssetId): F[AssetInfo]
  def downloadObject(assetId: AssetId): F[(NonEmptyString, fs2.Stream[F, Byte])]
}
object AssetsAlgebra {
  def make[F[_]: MonadThrow: GenUUID: Calendar: Lambda[M[_] => fs2.Compiler[M, M]]](
      assetsRepository: AssetsRepository[F],
      s3Client: S3Client[F],
    ): AssetsAlgebra[F] =
    new AssetsAlgebra[F] {
      override def create(
          assetInfo: AssetInput,
          fileKey: String,
          mediaType: String,
        ): F[AssetId] =
        for {
          id <- ID.make[F, AssetId]
          now <- Calendar[F].currentZonedDateTime
          asset = Asset(
            id = id,
            createdAt = now,
            s3Key = fileKey,
            public = assetInfo.public,
            fileName = assetInfo.filename,
            mediaType = mediaType,
          )
          _ <- assetsRepository.create(asset)
        } yield id

      override def uploadFile(
          parts: Vector[Part[F]],
          public: Boolean,
        ): F[Option[(String, String)]] = {
        println(parts.map(_.headers.get[`Content-Type`]))
        val files = parts.flatMap { part =>
          for {
            contentType <- part.headers.get[`Content-Type`]
            filename <- part.filename
            mediaType = s"${contentType.mediaType.mainType}/${contentType.mediaType.subType}"
          } yield filename -> mediaType -> part.body
        }
        files
          .traverse {
            case filename -> mediaType -> body =>
              body.through(uploadToS3(filename, public)).map(mediaType -> _)
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
            AssetInfo(asset.public, asset.fileName, asset.mediaType, url)
          }
        }

      override def downloadObject(assetId: AssetId): F[(NonEmptyString, fs2.Stream[F, Byte])] =
        OptionT(assetsRepository.findAsset(assetId)).foldF(
          AError
            .Internal(s"File not found by assetId [$assetId]")
            .raiseError[F, (NonEmptyString, fs2.Stream[F, Byte])]
        ) { asset =>
          (asset.mediaType -> s3Client.downloadObject(asset.s3Key.value)).pure[F]
        }

      private def getFileType(filename: String): String = {
        val extension = filename.substring(filename.lastIndexOf('.') + 1)
        extension.toLowerCase
      }

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
