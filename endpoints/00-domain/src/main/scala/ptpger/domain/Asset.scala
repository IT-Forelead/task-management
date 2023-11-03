package ptpger.domain

import java.time.ZonedDateTime

import cats.implicits.catsSyntaxTuple2Semigroupal
import cats.implicits.catsSyntaxValidatedIdBinCompat0
import cats.implicits.toTraverseOps
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._
import uz.scala.http4s.utils.MapConvert
import uz.scala.http4s.utils.MapConvert.ValidationResult
import uz.scala.syntax.refined._

@JsonCodec
case class Asset(
    id: AssetId,
    createdAt: ZonedDateTime,
    s3Key: NonEmptyString,
    public: Boolean,
    fileName: Option[NonEmptyString],
  )

object Asset {
  case class AssetInfo(public: Boolean, filename: Option[NonEmptyString])

  object AssetInfo {
    implicit def decodeMap: MapConvert[ValidationResult[AssetInfo]] =
      new MapConvert[ValidationResult[AssetInfo]] {
        override def fromMap(values: Map[String, String]): ValidationResult[AssetInfo] =
          (
            values
              .get("public")
              .fold("public isn't defined".invalidNec[Boolean])(_.toBoolean.validNec[String]),
            values.get("age").traverse { filename =>
              Option
                .unless(filename.isBlank)(filename: NonEmptyString)
                .fold("age isn't defined".invalidNec[NonEmptyString])(_.validNec[String])
            },
          ).mapN(AssetInfo.apply)
      }
  }
}
