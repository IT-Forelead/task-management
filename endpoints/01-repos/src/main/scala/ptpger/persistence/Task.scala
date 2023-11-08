package ptpger.persistence

import java.time.LocalDate
import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import ptpger.domain.AssetId
import ptpger.domain.TaskId
import ptpger.domain.enums.TaskStatus

@JsonCodec
case class Task(
    id: TaskId,
    createdAt: ZonedDateTime,
    title: NonEmptyString,
    dueDate: LocalDate,
    status: TaskStatus,
    description: NonEmptyString,
    assetId: Option[AssetId],
  )
