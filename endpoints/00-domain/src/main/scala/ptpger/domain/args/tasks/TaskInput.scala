package ptpger.domain
package args.tasks

import java.time.LocalDate

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class TaskInput(
    title: NonEmptyString,
    assetId: AssetId,
    dueDate: LocalDate,
    description: NonEmptyString,
  )
