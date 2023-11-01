package ptpger.repos

import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxMonadError
import skunk._
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps

import ptpger.domain.Comment
import ptpger.domain.TaskId
import ptpger.exception.AError
import ptpger.repos.sql.TaskCommentsSql
trait TaskCommentsRepository[F[_]] {
  def create(comment: Comment): F[Unit]
  def get(taskId: TaskId): F[List[Comment]]
}

object TaskCommentsRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): TaskCommentsRepository[F] = new TaskCommentsRepository[F] {
    override def create(comment: Comment): F[Unit] =
      TaskCommentsSql
        .insert
        .execute(comment)
        .adaptError {
          case SqlState.ForeignKeyViolation(_) =>
            AError.Internal(s"Incorrect task id entered [${comment.taskId}")
        }
    override def get(taskId: TaskId): F[List[Comment]] =
      TaskCommentsSql.select.queryList(taskId)
  }
}
