package ptpger.repos.sql

import skunk._
import skunk.implicits._

import ptpger.domain.ActionHistory
import ptpger.domain.TaskId
private[repos] object ActionHistoriesSql extends Sql[TaskId] {
  private val codec =
    (id *: zonedDateTime *: action *: nes *: UsersSql.id).to[ActionHistory]

  val insert: Command[ActionHistory] =
    sql"""INSERT INTO action_histories VALUES ($codec)""".command

  val select: Query[TaskId, ActionHistory] =
    sql"""SELECT * FROM action_histories WHERE task_id = $id""".query(codec)
}
