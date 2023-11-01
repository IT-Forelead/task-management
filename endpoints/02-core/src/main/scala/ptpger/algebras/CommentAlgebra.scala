package ptpger.algebras

import cats.Monad
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps

import ptpger.domain._
import ptpger.effects.Calendar
import ptpger.effects.GenUUID
import ptpger.repos.TaskCommentsRepository

trait CommentAlgebra[F[_]] {
  def create(userId: PersonId, comment: CommentInput): F[Unit]
  def get(taskId: TaskId): F[List[Comment]]
}
object CommentAlgebra {
  def make[F[_]: Monad: Calendar: GenUUID](
      taskCommentsRepository: TaskCommentsRepository[F]
    ): CommentAlgebra[F] =
    new CommentAlgebra[F] {
      override def create(userId: PersonId, commentInput: CommentInput): F[Unit] =
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
      override def get(taskId: TaskId): F[List[Comment]] =
        taskCommentsRepository.get(taskId)
    }
}
