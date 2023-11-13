package ptpger.domain
package args.tasks

import java.time.LocalDate

import cats.data.NonEmptyList
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class TaskInput(
    title: NonEmptyString,
    assetId: Option[AssetId],
    dueDate: LocalDate,
    description: NonEmptyString,
    assigned: Option[NonEmptyList[PersonId]],
  )
