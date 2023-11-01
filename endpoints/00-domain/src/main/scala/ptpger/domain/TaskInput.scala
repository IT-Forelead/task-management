package ptpger.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class TaskInput(
    title: NonEmptyString,
    filename: NonEmptyString,
    dueDate: ZonedDateTime,
    description: NonEmptyString,
  )
