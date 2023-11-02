package ptpger.repos.sql

import skunk._
import skunk.implicits._

import ptpger.domain.Comment
import ptpger.domain.TaskId
private[repos] object TaskCommentsSql extends Sql[TaskId] {
  private val codec =
    (id *: zonedDateTime *: nes *: UsersSql.id).to[Comment]

  val insert: Command[Comment] =
    sql"""INSERT INTO task_comments VALUES ($codec)""".command

  val select: Query[TaskId, Comment] =
    sql"""SELECT * FROM task_comments WHERE task_id = $id""".query(codec)
}
