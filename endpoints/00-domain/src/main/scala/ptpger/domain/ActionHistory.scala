package ptpger.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import ptpger.domain.enums.Action

@JsonCodec
case class ActionHistory(
    taskId: TaskId,
    createdAt: ZonedDateTime,
    action: Action,
    description: NonEmptyString,
    userId: PersonId,
  )
