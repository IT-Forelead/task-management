package ptpger.repos

import cats.effect.Async
import cats.effect.Resource
import cats.implicits.catsSyntaxMonadError
import skunk._
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps

import ptpger.domain.ActionHistory
import ptpger.domain.TaskId
import ptpger.exception.AError
import ptpger.repos.sql.ActionHistoriesSql
trait ActionHistoriesRepository[F[_]] {
  def create(actionHistory: ActionHistory): F[Unit]
  def get(taskId: TaskId): F[List[ActionHistory]]
}

object ActionHistoriesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): ActionHistoriesRepository[F] = new ActionHistoriesRepository[F] {
    override def create(actionHistory: ActionHistory): F[Unit] =
      ActionHistoriesSql
        .insert
        .execute(actionHistory)
        .adaptError {
          case SqlState.ForeignKeyViolation(_) =>
            AError.Internal(s"Incorrect task id entered [${actionHistory.taskId}")
        }
    override def get(taskId: TaskId): F[List[ActionHistory]] =
      ActionHistoriesSql.select.queryList(taskId)
  }
}
