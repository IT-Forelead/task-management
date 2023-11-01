package ptpger.repos.sql

import ptpger.domain.Task
import ptpger.domain.TaskId

private[repos] object TasksSql extends Sql[TaskId] {
  private val codec =
    (id *: zonedDateTime *: nes *: nes *: zonedDateTime *: UsersSql.id.opt *: status *: nes)
      .to[Task]
}
