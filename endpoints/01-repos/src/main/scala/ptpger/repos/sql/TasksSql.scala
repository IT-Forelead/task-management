package ptpger.repos.sql

import skunk._
import skunk.codec.all.date
import skunk.codec.all.int8
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import ptpger.domain.Counts
import ptpger.domain.TaskId
import ptpger.domain.PersonId
import ptpger.domain.args.tasks.TaskFilters
import ptpger.persistence.Task
private[repos] object TasksSql extends Sql[TaskId] {
  private[repos] val codec =
    (id *: zonedDateTime *: nes *: date *: status *: nes *: AssetsSql.id.opt)
      .to[Task]

  private[repos] val countsCodec =
    (int8 *: int8 *: int8 *: int8 *: int8 *: int8 *: int8 *: int8)
      .to[Counts]

  val insert: Command[Task] =
    sql"""INSERT INTO tasks VALUES ($codec)""".command

  private def searchFilter(filters: TaskFilters): List[Option[AppliedFragment]] =
    List(
      filters
        .assigned
        .flatMap(cond => Option.when(cond)(()))
        .map(_ => sql"user_id IS NOT NULL".apply(Void)),
//      filters.userId.map(sql"user_id = ${UsersSql.id}"),
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
          COUNT(1) filter (WHERE status = 'complete') as completed,
          COUNT(1) filter (WHERE status = 'on_hold') as on_hold,
          COUNT(1) filter (WHERE status = 'rejected') as rejected,
          COUNT(1) filter (WHERE status = 'approved') as approved,
          COUNT(1) filter (WHERE status = 'expired') as expired
          FROM tasks
       """.query(countsCodec)

  // TODO remove code duplication
  val countByUser: Query[PersonId, Counts] =
    sql"""SELECT
          COUNT(1) as count,
          COUNT(1) filter (WHERE status = 'new') as "new",
          COUNT(1) filter (WHERE status = 'in_progress') as in_progress,
          COUNT(1) filter (WHERE status = 'complete') as completed,
          COUNT(1) filter (WHERE status = 'on_hold') as on_hold,
          COUNT(1) filter (WHERE status = 'rejected') as rejected,
          COUNT(1) filter (WHERE status = 'approved') as approved,
          COUNT(1) filter (WHERE status = 'expired') as expired
          FROM tasks
          INNER JOIN user_tasks
          ON tasks.id = user_tasks.task_id
          WHERE user_id  = ${UsersSql.id}
      """.query(countsCodec)

  val findById: Query[TaskId, Task] =
    sql"""SELECT * FROM tasks WHERE id = $id LIMIT 1""".query(codec)

  val update: Command[Task] =
    sql"""UPDATE tasks 
         SET title = $nes,
         asset_id = ${AssetsSql.id.opt},
         due_date = $date,
         status = $status,
         description = $nes
         WHERE id = $id
       """
      .command
      .contramap {
        case task: Task =>
          task.title *: task.assetId *: task.dueDate *: task.status *: task.description *: task.id *: EmptyTuple

      }
}
