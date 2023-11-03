package ptpger.algebras

import cats.MonadThrow
import cats.data.OptionT
import cats.implicits.catsSyntaxApplicativeByName
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import ptpger.domain._
import ptpger.domain.enums.Action
import ptpger.domain.enums.Assignment
import ptpger.domain.enums.Assignment.Assigned
import ptpger.domain.enums.Assignment.Unassigned
import ptpger.domain.enums.TaskStatus
import ptpger.effects.Calendar
import ptpger.effects.GenUUID
import ptpger.exception.AError
import ptpger.repos.ActionHistoriesRepository
import ptpger.repos.TaskCommentsRepository
import ptpger.repos.TasksRepository
import ptpger.repos.UsersRepository
import ptpger.utils.ID

trait TaskAlgebra[F[_]] {
  def create(taskInput: TaskInput): F[TaskId]
  def get: F[List[Task]]
  def update(
      id: TaskId,
      userId: PersonId,
      taskInput: TaskUpdateInput,
    ): F[Unit]

  def addComment(userId: PersonId, comment: CommentInput): F[Unit]
  def getComments(taskId: TaskId): F[List[Comment]]
  def getActionHistories(taskId: TaskId): F[List[ActionHistory]]
}
object TaskAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      tasksRepository: TasksRepository[F],
      taskCommentsRepository: TaskCommentsRepository[F],
      actionHistoriesRepository: ActionHistoriesRepository[F],
      usersRepository: UsersRepository[F],
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
            assetId = taskInput.assetId,
            dueDate = taskInput.dueDate,
            userId = None,
            status = TaskStatus.New,
            description = taskInput.description,
          )
          _ <- tasksRepository.create(task)
        } yield id

      override def get: F[List[Task]] =
        tasksRepository.get

      override def update(
          id: TaskId,
          userId: PersonId,
          taskInput: TaskUpdateInput,
        ): F[Unit] =
        tasksRepository.update(id) { task =>
          for {
            _ <- taskAssignment(task.id, taskInput.userId, userId, Assigned).whenA(
              task.userId.isEmpty && taskInput.userId.nonEmpty
            )
            _ <- taskAssignment(task.id, taskInput.userId, userId, Unassigned).whenA(
              task.userId.nonEmpty && taskInput.userId.isEmpty
            )
            _ <- changeStatus(task.id, userId, taskInput.status).whenA(
              task.status != taskInput.status
            )
          } yield task.copy(
            title = taskInput.title,
            assetId = taskInput.assetId,
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

      override def getActionHistories(taskId: TaskId): F[List[ActionHistory]] =
        actionHistoriesRepository.get(taskId)

      private def taskAssignment(
          taskId: TaskId,
          executorId: Option[PersonId],
          assignerId: PersonId,
          assignment: Assignment,
        ): F[Unit] =
        for {
          now <- Calendar[F].currentZonedDateTime
          user <- OptionT(executorId.flatTraverse(usersRepository.findById))
            .getOrRaise(AError.Internal("User by id not found"))
          action = ActionHistory(
            taskId = taskId,
            createdAt = now,
            userId = assignerId,
            action = Action.Assignment,
            description = assignment.description(user.firstname, user.lastname),
          )
          _ <- actionHistoriesRepository.create(action)
        } yield {}

      private def changeStatus(
          taskId: TaskId,
          changerId: PersonId,
          status: TaskStatus,
        ): F[Unit] =
        for {
          now <- Calendar[F].currentZonedDateTime
          action = ActionHistory(
            taskId = taskId,
            createdAt = now,
            userId = changerId,
            action = Action.ChangeStatus,
            description = s"Vazifa statusi $status ga o'zgartirildi",
          )
          _ <- actionHistoriesRepository.create(action)
        } yield {}
    }
}
