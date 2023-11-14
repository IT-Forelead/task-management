package ptpger.repos

import cats.effect.Async
import cats.effect.Resource
import cats.implicits.toFunctorOps
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import ptpger.domain.Message
import ptpger.domain.MessageId
import ptpger.domain.ResponseData
import ptpger.domain.args.messages.MessageFilter
import ptpger.domain.enums.DeliveryStatus
import ptpger.repos.sql.MessagesSql
trait MessagesRepository[F[_]] {
  def create(message: Message): F[Unit]
  def update(messageId: MessageId)(status: DeliveryStatus): F[Unit]
  def get(filter: MessageFilter): F[ResponseData[Message]]
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
    override def get(filters: MessageFilter): F[ResponseData[Message]] = {
      val query =
        MessagesSql.get.paginateOpt(filters.limit.map(_.value), filters.offset.map(_.value))
      query.fragment.query(MessagesSql.codec *: int8).queryList(query.argument).map { messages =>
        ResponseData(messages.map(_.head), messages.headOption.fold(0L)(_.tail.head))
      }
    }
  }
}
