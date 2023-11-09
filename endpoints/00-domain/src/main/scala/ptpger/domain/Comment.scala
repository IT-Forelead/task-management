package ptpger.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class Comment(
    taskId: TaskId,
    createdAt: ZonedDateTime,
    note: NonEmptyString,
    userId: PersonId,
    assetId: Option[AssetId],
  )
