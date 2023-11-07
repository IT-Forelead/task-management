package ptpger.domain.args.tasks

import cats.data.NonEmptyList
import io.circe.generic.JsonCodec

import ptpger.domain.PersonId

@JsonCodec
case class TaskAssignInput(
    userIds: NonEmptyList[PersonId]
  )
