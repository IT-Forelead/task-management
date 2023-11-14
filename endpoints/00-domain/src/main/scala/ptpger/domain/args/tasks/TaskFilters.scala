package ptpger.domain
package args.tasks

import eu.timepit.refined.types.all.NonNegInt
import io.circe.generic.JsonCodec
import io.circe.refined._

import ptpger.domain.PersonId
import ptpger.domain.args.DateRange
import ptpger.domain.args.DateTimeRange
import ptpger.domain.enums.TaskStatus

@JsonCodec
case class TaskFilters(
    userId: Option[PersonId] = None,
    status: Option[TaskStatus] = None,
    dueDate: Option[DateRange] = None,
    createdAt: Option[DateTimeRange] = None,
    assigned: Option[Boolean] = None,
    limit: Option[NonNegInt],
    offset: Option[NonNegInt],
  )
