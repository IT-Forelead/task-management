package ptpger.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import ptpger.domain.enums.TaskStatus
import io.circe.refined._

import java.time.ZonedDateTime
@JsonCodec
case class TaskUpdateInput(
    title: NonEmptyString,
    filename: NonEmptyString,
    dueDate: ZonedDateTime,
    userId: Option[PersonId],
    status: TaskStatus,
    description: NonEmptyString,
  )
