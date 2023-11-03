package ptpger.domain
package args.tasks

import io.circe.generic.JsonCodec
import ptpger.domain.PersonId
import ptpger.domain.args.{DateRange, DateTimeRange}
import ptpger.domain.enums.TaskStatus

@JsonCodec
case class TaskFilters(
    userId: Option[PersonId] = None,
    status: Option[TaskStatus] = None,
    dueDate: Option[DateRange] = None,
    createdAt: Option[DateTimeRange] = None,
    assigned: Option[Boolean] = None
  )
