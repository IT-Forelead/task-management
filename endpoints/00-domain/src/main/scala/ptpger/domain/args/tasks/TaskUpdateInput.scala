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
    assetId: Option[AssetId],
    dueDate: LocalDate,
    status: TaskStatus,
    description: NonEmptyString,
  )
