package ptpger.algebras

import cats.Monad
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps

import ptpger.domain._
import ptpger.domain.enums.TaskStatus
import ptpger.effects.Calendar
import ptpger.effects.GenUUID
import ptpger.repos.TasksRepository
import ptpger.utils.ID

trait TaskAlgebra[F[_]] {
  def create(taskInput: TaskInput): F[TaskId]
  def get: F[List[Task]]
  def update(id: TaskId, taskInput: TaskUpdateInput): F[Unit]
}
object TaskAlgebra {
  def make[F[_]: Monad: Calendar: GenUUID](tasksRepository: TasksRepository[F]): TaskAlgebra[F] =
    new TaskAlgebra[F] {
      override def create(taskInput: TaskInput): F[TaskId] =
        for {
          id <- ID.make[F, TaskId]
          now <- Calendar[F].currentZonedDateTime
          task = Task(
            id = id,
            createdAt = now,
            title = taskInput.title,
            filename = taskInput.filename,
            dueDate = taskInput.dueDate,
            userId = None,
            status = TaskStatus.New,
            description = taskInput.description,
          )
          _ <- tasksRepository.create(task)
        } yield id
      override def get: F[List[Task]] =
        tasksRepository.get
      override def update(id: TaskId, taskInput: TaskUpdateInput): F[Unit] =
        tasksRepository.update(id)(
          _.copy(
            title = taskInput.title,
            filename = taskInput.filename,
            dueDate = taskInput.dueDate,
            userId = taskInput.userId,
            status = taskInput.status,
            description = taskInput.description,
          )
        )
    }
}
