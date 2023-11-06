package ptpger.repos.sql

import skunk._
import skunk.codec.all.date
import skunk.implicits._
import skunk.codec.all.int4
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import ptpger.domain.Task
import ptpger.domain.TaskId
import ptpger.domain.Counts
import ptpger.domain.args.tasks.TaskFilters
private[repos] object TasksSql extends Sql[TaskId] {
  private[repos] val codec =
    (id *: zonedDateTime *: nes *: date *: UsersSql.id.opt *: status *: nes *: AssetsSql.id)
      .to[Task]

  private[repos] val countsCodec =
    (int4 *: int4 *: int4 *: int4 *: int4 *: int4 *: int4)
      .to[Counts]

  val insert: Command[Task] =
    sql"""INSERT INTO tasks VALUES ($codec)""".command

  private def searchFilter(filters: TaskFilters): List[Option[AppliedFragment]] =
    List(
      filters
        .assigned
        .flatMap(cond => Option.when(cond)(()))
        .map(_ => sql"user_id IS NOT NULL".apply(Void)),
      filters.userId.map(sql"user_id = ${UsersSql.id}"),
      filters.status.map(sql"status = $status"),
    ) :::
      filters.dueDate.toList.flatMap { dateRange =>
        List(
          dateRange.from.map(sql"due_date >= $date"),
          dateRange.to.map(sql"due_date <= $date"),
        )
      } :::
      filters.createdAt.toList.flatMap { dateRange =>
        List(
          dateRange.from.map(sql"created_at >= $zonedDateTime"),
          dateRange.to.map(sql"created_at <= $zonedDateTime"),
        )
      }

  def select(filters: TaskFilters): AppliedFragment = {
    val baseQuery: Fragment[Void] = sql"""SELECT * FROM tasks"""
    baseQuery(Void).whereAndOpt(searchFilter(filters))
  }

  val count: Query[Void, Counts] =
    sql"""SELECT
         COUNT(1) as count,
         COUNT(1) filter (WHERE status = 'new') as "new",
         COUNT(1) filter (WHERE status = 'in_progress') as in_progress,
         COUNT(1) filter (WHERE status = 'completed') as completed,
         COUNT(1) filter (WHERE status = 'on_hold') as on_hold,
         COUNT(1) filter (WHERE status = 'rejected') as rejected,
         COUNT(1) filter (WHERE status = 'approved') as approved,
         FROM tasks
       """.query(countsCodec)

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
