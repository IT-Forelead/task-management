package ptpger.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString

import ptpger.domain.enums.TaskStatus

case class Task(
    id: TaskId,
    createdAt: ZonedDateTime,
    title: NonEmptyString,
    filename: NonEmptyString,
    dueDate: ZonedDateTime,
    userId: Option[PersonId],
    status: TaskStatus,
    description: NonEmptyString,
  )
