package ptpger.domain
package args.tasks

import java.time.LocalDate

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import ptpger.domain.enums.TaskStatus

@JsonCodec
case class TaskUpdateInput(
    title: NonEmptyString,
    assetId: AssetId,
    dueDate: LocalDate,
    userId: Option[PersonId],
    status: TaskStatus,
    description: NonEmptyString,
  )
