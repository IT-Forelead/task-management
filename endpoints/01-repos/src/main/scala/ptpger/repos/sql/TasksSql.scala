package ptpger.repos.sql

import skunk._
import skunk.codec.all.date
import skunk.codec.all.int8
import skunk.codec.all.varchar
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import ptpger.domain.Counts
import ptpger.domain.CountsAll
import ptpger.domain.PersonId
import ptpger.domain.TaskId
import ptpger.domain.args.tasks.TaskFilters
import ptpger.persistence.Task
private[repos] object TasksSql extends Sql[TaskId] {
  private[repos] val codec =
    (id *: zonedDateTime *: nes *: date *: status *: nes *: AssetsSql.id.opt)
      .to[Task]

  private[repos] val countsCodec =
    (int8 *: int8 *: int8 *: int8 *: int8 *: int8 *: int8 *: int8)
      .to[Counts]

  private[repos] val countsAllCodec =
    (varchar *: varchar *: int8 *: int8 *: int8 *: int8 *: int8 *: int8 *: int8 *: int8)
      .to[CountsAll]

  val insert: Command[Task] =
    sql"""INSERT INTO tasks VALUES ($codec)""".command

  private def searchFilter(filters: TaskFilters): List[Option[AppliedFragment]] =
    List(
      filters
        .assigned
        .flatMap(cond => Option.when(cond)(()))
        .map(_ => sql"ut.user_id IS NOT NULL".apply(Void)),
      filters.userId.map(sql"ut.user_id = ${UsersSql.id}"),
      filters.status.map(sql"t.status = $status"),
    ) :::
      filters.dueDate.toList.flatMap { dateRange =>
        List(
          dateRange.from.map(sql"t.due_date >= $date"),
          dateRange.to.map(sql"t.due_date <= $date"),
        )
      } :::
      filters.createdAt.toList.flatMap { dateRange =>
        List(
          dateRange.from.map(sql"t.created_at >= $zonedDateTime"),
          dateRange.to.map(sql"t.created_at <= $zonedDateTime"),
        )
      }

  def select(filters: TaskFilters): AppliedFragment = {
    val baseQuery: Fragment[Void] =
      sql"""SELECT DISTINCT ON(t.id) t.*, COUNT(*) OVER() AS total FROM tasks t
            LEFT JOIN user_tasks ut ON t.id = ut.task_id or ut.user_id IS NULL"""
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

  val countAll: Query[Void, CountsAll] =
    sql"""SELECT
            users.firstname,
            users.lastname,
            COUNT(1) AS count,
            COUNT(1) FILTER (WHERE status = 'new') AS "new",
            COUNT(1) FILTER (WHERE status = 'in_progress') AS in_progress,
            COUNT(1) FILTER (WHERE status = 'complete') AS completed,
            COUNT(1) FILTER (WHERE status = 'on_hold') AS on_hold,
            COUNT(1) FILTER (WHERE status = 'rejected') AS rejected,
            COUNT(1) FILTER (WHERE status = 'approved') AS approved,
            COUNT(1) FILTER (WHERE status = 'expired') AS expired
          FROM tasks
          INNER JOIN user_tasks
            ON tasks.id = user_tasks.task_id
          INNER JOIN users
            ON user_tasks.user_id = users.id
          GROUP BY users.id
         """.query(countsAllCodec)

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
