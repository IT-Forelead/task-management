package ptpger.domain

import java.time.LocalDate
import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import ptpger.domain.enums.TaskStatus
@JsonCodec
case class Task(
    id: TaskId,
    createdAt: ZonedDateTime,
    title: NonEmptyString,
    filename: NonEmptyString,
    dueDate: LocalDate,
    userId: Option[PersonId],
    status: TaskStatus,
    description: NonEmptyString,
  )
