package ptpger.domain

import java.time.LocalDate

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class TaskInput(
    title: NonEmptyString,
    filename: NonEmptyString,
    dueDate: LocalDate,
    description: NonEmptyString,
  )
