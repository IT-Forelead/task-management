package ptpger.repos.sql

import skunk._
import skunk.codec.all.date
import skunk.implicits._

import ptpger.domain.Task
import ptpger.domain.TaskId
private[repos] object TasksSql extends Sql[TaskId] {
  private val codec =
    (id *: zonedDateTime *: nes *: date *: UsersSql.id.opt *: status *: nes *: AssetsSql.id)
      .to[Task]

  val insert: Command[Task] =
    sql"""INSERT INTO tasks VALUES ($codec)""".command

  val select: Query[Void, Task] =
    sql"""SELECT * FROM tasks""".query(codec)

  val findById: Query[TaskId, Task] =
    sql"""SELECT * FROM tasks WHERE id = $id LIMIT 1""".query(codec)

  val update: Command[Task] =
    sql"""UPDATE tasks 
         SET title = $nes,
         asset_id = ${AssetsSql.id},
         due_date = $date,
         user_id = ${UsersSql.id.opt},
         status = $status,
         description = $nes
         WHERE id = $id
       """
      .command
      .contramap {
        case task: Task =>
          task.title *: task.assetId *: task.dueDate *: task.userId *: task.status *: task.description *: task.id *: EmptyTuple

      }
}
