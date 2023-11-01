package ptpger.algebras

import cats.Applicative
import cats.Monad
import cats.implicits.catsSyntaxApplicativeByName
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps

import ptpger.domain._
import ptpger.domain.enums.TaskStatus
import ptpger.effects.Calendar
import ptpger.effects.GenUUID
import ptpger.repos.TaskCommentsRepository
import ptpger.repos.TasksRepository
import ptpger.utils.ID

trait TaskAlgebra[F[_]] {
  def create(taskInput: TaskInput): F[TaskId]
  def get: F[List[Task]]
  def update(id: TaskId, taskInput: TaskUpdateInput): F[Unit]

  def addComment(userId: PersonId, comment: CommentInput): F[Unit]
  def getComments(taskId: TaskId): F[List[Comment]]
}
object TaskAlgebra {
  def make[F[_]: Monad: Calendar: GenUUID](
      tasksRepository: TasksRepository[F],
      taskCommentsRepository: TaskCommentsRepository[F],
    ): TaskAlgebra[F] =
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
        tasksRepository.update(id) { task =>
          for {
            _ <- assignTask(task).whenA(task.userId.isEmpty && taskInput.userId.nonEmpty)
            _ <- changeStatus(task).whenA(task.status != taskInput.status)
          } yield task.copy(
            title = taskInput.title,
            filename = taskInput.filename,
            dueDate = taskInput.dueDate,
            userId = taskInput.userId,
            status = taskInput.status,
            description = taskInput.description,
          )
        }

      override def addComment(userId: PersonId, commentInput: CommentInput): F[Unit] =
        for {
          now <- Calendar[F].currentZonedDateTime
          comment = Comment(
            taskId = commentInput.taskId,
            createdAt = now,
            note = commentInput.note,
            userId = userId,
          )
          _ <- taskCommentsRepository.create(comment)
        } yield {}

      override def getComments(taskId: TaskId): F[List[Comment]] =
        taskCommentsRepository.get(taskId)

      private def assignTask(task: Task): F[Unit] = Applicative[F].unit
      private def changeStatus(task: Task): F[Unit] = Applicative[F].unit
    }
}
