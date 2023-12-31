package ptpger.domain

import java.net.URL
import java.time.ZonedDateTime

import cats.implicits.catsSyntaxTuple2Semigroupal
import cats.implicits.catsSyntaxValidatedIdBinCompat0
import cats.implicits.toTraverseOps
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import uz.scala.http4s.utils.MapConvert
import uz.scala.http4s.utils.MapConvert.ValidationResult
import uz.scala.syntax.circe._
import uz.scala.syntax.refined._

@JsonCodec
case class Asset(
    id: AssetId,
    createdAt: ZonedDateTime,
    s3Key: NonEmptyString,
    public: Boolean,
    fileName: Option[NonEmptyString],
    mediaType: NonEmptyString,
  )

object Asset {
  case class AssetInput(public: Boolean, filename: Option[NonEmptyString])
  @JsonCodec
  case class AssetInfo(
      public: Boolean,
      filename: Option[NonEmptyString],
      mediaType: NonEmptyString,
      extension: NonEmptyString,
      url: URL,
    )

  object AssetInput {
    implicit def decodeMap: MapConvert[ValidationResult[AssetInput]] =
      new MapConvert[ValidationResult[AssetInput]] {
        override def fromMap(values: Map[String, String]): ValidationResult[AssetInput] =
          (
            values
              .get("public")
              .fold("public isn't defined".invalidNec[Boolean])(_.toBoolean.validNec[String]),
            values.get("filename").traverse { filename =>
              Option
                .unless(filename.isBlank)(filename: NonEmptyString)
                .fold("filename isn't defined".invalidNec[NonEmptyString])(_.validNec[String])
            },
          ).mapN(AssetInput.apply)
      }
  }
}
