package ptpger.domain.args.tasks

import io.circe.generic.JsonCodec

import ptpger.domain.PersonId

@JsonCodec
case class TaskAssignInput(
    userId: PersonId
  )
