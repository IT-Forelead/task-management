package ptpger.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps

import ptpger.domain.Message
import ptpger.domain.MessageId
import ptpger.domain.enums.DeliveryStatus
import ptpger.repos.sql.MessagesSql
trait MessagesRepository[F[_]] {
  def create(message: Message): F[Unit]
  def update(messageId: MessageId)(status: DeliveryStatus): F[Unit]
}

object MessagesRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): MessagesRepository[F] = new MessagesRepository[F] {
    override def create(message: Message): F[Unit] =
      MessagesSql.insert.execute(message)
    override def update(messageId: MessageId)(status: DeliveryStatus): F[Unit] =
      MessagesSql.update.execute(status *: messageId *: EmptyTuple)
  }
}
