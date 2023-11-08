package ptpger.repos

import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.catsSyntaxMonadError
import cats.implicits.toFlatMapOps
import skunk._
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps

import ptpger.domain.Counts
import ptpger.domain.Task
import ptpger.domain.TaskId
import ptpger.domain.UserTask
import ptpger.domain.args.tasks.TaskFilters
import ptpger.exception.AError
import ptpger.repos.sql.TasksSql
import ptpger.repos.sql.UserTasksSql
trait TasksRepository[F[_]] {
  def create(task: Task): F[Unit]
  def get(filters: TaskFilters): F[List[Task]]
  def getCounts: F[Counts]
  def findById(taskId: TaskId): F[Option[Task]]
  def update(id: TaskId)(update: Task => F[Task]): F[Unit]
  def assign(userTasks: NonEmptyList[UserTask]): F[Unit]
}

object TasksRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TasksRepository[F] = new TasksRepository[F] {
    override def create(task: Task): F[Unit] =
      TasksSql.insert.execute(task)
    override def get(filters: TaskFilters): F[List[Task]] = {
      val query = TasksSql.select(filters)
      query.fragment.query(TasksSql.codec).queryList(query.argument)
    }
    override def getCounts: F[Counts] =
      TasksSql.count.queryUnique(Void)
    override def findById(taskId: TaskId): F[Option[Task]] =
      TasksSql.findById.queryOption(taskId)
    override def update(id: TaskId)(update: Task => F[Task]): F[Unit] =
      OptionT(findById(id)).cataF(
        AError.Internal(s"Task not found by id [$id]").raiseError[F, Unit],
        task =>
          update(task).flatMap { updatedTask =>
            TasksSql.update.execute(updatedTask)
          },
      )
    override def assign(userTasks: NonEmptyList[UserTask]): F[Unit] = {
      val tasks = userTasks.toList
      UserTasksSql.insertBatch(tasks).execute(tasks).adaptError {
        case SqlState.UniqueViolation(_) =>
          AError.Internal("Task already assigned")
      }
    }
  }
}
