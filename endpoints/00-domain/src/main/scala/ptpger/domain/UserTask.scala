package ptpger.domain

import io.circe.generic.JsonCodec

@JsonCodec
case class UserTask(taskId: TaskId, userId: PersonId)
