package ptpger.repos

import cats.data.OptionT
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxApplicativeErrorId
import cats.implicits.toFlatMapOps
import skunk._
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps

import ptpger.domain.Task
import ptpger.domain.TaskId
import ptpger.domain.args.tasks.TaskFilters
import ptpger.exception.AError
import ptpger.repos.sql.TasksSql
trait TasksRepository[F[_]] {
  def create(task: Task): F[Unit]
  def get(filters: TaskFilters): F[List[Task]]
  def findById(taskId: TaskId): F[Option[Task]]
  def update(id: TaskId)(update: Task => F[Task]): F[Unit]
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
  }
}
