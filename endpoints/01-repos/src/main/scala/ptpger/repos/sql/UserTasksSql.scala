package ptpger.repos.sql

import skunk._
import skunk.implicits._

import ptpger.domain.TaskId
import ptpger.domain.UserTask
private[repos] object UserTasksSql extends Sql[TaskId] {
  private[repos] val codec =
    (id *: UsersSql.id).to[UserTask]
  def insertBatch(userTasks: List[UserTask]): Command[userTasks.type] =
    sql"""INSERT INTO user_tasks VALUES ${codec.values.list(userTasks)}""".command

  val select: Query[TaskId, UserTask] =
    sql"""SELECT * FROM user_tasks WHERE task_id = $id""".query(codec)
}
