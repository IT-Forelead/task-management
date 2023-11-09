package ptpger.algebras

import cats.Applicative
import cats.MonadThrow
import cats.data.NonEmptyList
import cats.data.OptionT
import cats.implicits._
import cats.implicits.catsSyntaxApplicativeByName
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
import eu.timepit.refined.types.string.NonEmptyString
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import ptpger.AppDomain
import ptpger.domain.ActionHistory
import ptpger.domain.Comment
import ptpger.domain.Counts
import ptpger.domain.PersonId
import ptpger.domain.TaskId
import ptpger.domain.UserTask
import ptpger.domain.args.tasks.CommentInput
import ptpger.domain.args.tasks.TaskFilters
import ptpger.domain.args.tasks.TaskInput
import ptpger.domain.args.tasks.TaskUpdateInput
import ptpger.domain.enums.Action
import ptpger.domain.enums.Role
import ptpger.domain.enums.TaskStatus
import ptpger.domain.{ Task => DTask }
import ptpger.effects.Calendar
import ptpger.effects.GenUUID
import ptpger.exception.AError
import ptpger.persistence.Task
import ptpger.repos.ActionHistoriesRepository
import ptpger.repos.TaskCommentsRepository
import ptpger.repos.TasksRepository
import ptpger.repos.UsersRepository
import ptpger.utils.ID

trait TaskAlgebra[F[_]] {
  def create(taskInput: TaskInput): F[TaskId]
  def get(filters: TaskFilters): F[List[DTask]]
  def getCounts: F[Counts]
  def getCountsByUserId(userId: PersonId): F[Counts]
  def update(
      id: TaskId,
      userId: PersonId,
      taskInput: TaskUpdateInput,
    ): F[Unit]

  def assign(
      taskId: TaskId,
      userIds: NonEmptyList[PersonId],
      author: NonEmptyString,
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
      messages: MessagesAlgebra[F],
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
            status = TaskStatus.New,
            description = taskInput.description,
          )
          _ <- tasksRepository.create(task)
        } yield id

      override def get(filters: TaskFilters): F[List[DTask]] =
        (for {
          tasks <- OptionT(tasksRepository.get(filters).map(NonEmptyList.fromList))
          userTasks <- OptionT.liftF(tasksRepository.getUserTasks(tasks.map(_.id)))
          maybeUserIds = NonEmptyList.fromList(
            userTasks.values.toList.flatten.map(_.userId).distinct
          )
          users <- OptionT.liftF(maybeUserIds.traverse(usersRepository.findByIds))
        } yield tasks.toList.map { task =>
          val taskUsers =
            userTasks.getOrElse(task.id, Nil).flatMap(ut => users.flatMap(_.get(ut.userId)))
          DTask(
            id = task.id,
            createdAt = task.createdAt,
            title = task.title,
            dueDate = task.dueDate,
            status = task.status,
            description = task.description,
            assetId = task.assetId,
            executors = taskUsers.filter(_.role == Role.Executor).map(_.fullName),
            controllers = taskUsers.filter(_.role == Role.Controller).map(_.fullName),
          )
        }).getOrElse(Nil)

      override def getCounts: F[Counts] =
        tasksRepository.getCounts

      override def getCountsByUserId(userId: PersonId): F[Counts] =
        tasksRepository.getCountsByUserId(userId)

      override def update(
          id: TaskId,
          userId: PersonId,
          taskInput: TaskUpdateInput,
        ): F[Unit] =
        tasksRepository.update(id) { task =>
          for {
            _ <- changeStatus(task.id, userId, taskInput.status).whenA(
              task.status != taskInput.status
            )
          } yield task.copy(
            title = taskInput.title,
            assetId = taskInput.assetId,
            dueDate = taskInput.dueDate,
            status = taskInput.status,
            description = taskInput.description,
          )
        }

      override def assign(
          taskId: TaskId,
          userIds: NonEmptyList[PersonId],
          author: NonEmptyString,
        ): F[Unit] = {
        val usersTask = userIds.map { userId =>
          UserTask(taskId, userId)
        }
        for {
          _ <- tasksRepository.assign(usersTask)
          _ <- taskAssignment(taskId, userIds, author)
        } yield {}
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
          executorIds: NonEmptyList[PersonId],
          author: NonEmptyString,
        ): F[Unit] =
        for {
          taskDetails <- OptionT(tasksRepository.findById(taskId))
            .getOrRaise(AError.Internal("Task by id not found"))
          now <- Calendar[F].currentZonedDateTime
          days <- Calendar[F].remainingDays(taskDetails.dueDate)
          users <- usersRepository.findByIds(executorIds)
          _ <- users.toList.traverse {
            case executorId -> user =>
              for {
                _ <- Applicative[F].unit
                action = ActionHistory(
                  taskId = taskId,
                  createdAt = now,
                  userId = executorId,
                  action = Action.Assignment,
                  description = author, // assignment.description(user.firstname, user.lastname),
                )
                linkToFile = taskDetails
                  .assetId
                  .map(id => s"Файлга ҳавола: $AppDomain/api/v1/assets/view/$id\n")
                  .getOrElse("")
                smsText = s"""
                             |Сизга топшириқ берилди:
                             |Топшириқ номи: ${taskDetails.title}
                             |Муддат тугашига қолган вақт: $days кун
                             |$linkToFile
                """.stripMargin.trim

                _ <- messages.sendSms(user.phone, smsText)
                _ <- actionHistoriesRepository.create(action)
              } yield {}
          }

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
