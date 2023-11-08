package ptpger.algebras

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import eu.timepit.refined.types.string.NonEmptyString

import ptpger.Phone
import ptpger.domain.Message
import ptpger.domain.MessageId
import ptpger.domain.enums.DeliveryStatus
import ptpger.effects.Calendar
import ptpger.effects.GenUUID
import ptpger.integrations.opersms.OperSmsClient
import ptpger.repos.MessagesRepository
import ptpger.utils.ID

trait MessagesAlgebra[F[_]] {
  def sendSms(phone: Phone, text: NonEmptyString): F[MessageId]
}
object MessagesAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      messagesRepository: MessagesRepository[F],
      operSmsClient: OperSmsClient[F],
    ): MessagesAlgebra[F] =
    new MessagesAlgebra[F] {
      override def sendSms(phone: Phone, text: NonEmptyString): F[MessageId] =
        for {
          id <- ID.make[F, MessageId]
          now <- Calendar[F].currentZonedDateTime
          message = Message(
            id = id,
            sentAt = now,
            phone = phone,
            text = text,
            status = DeliveryStatus.Sent,
          )
          _ <- messagesRepository.create(message)
          _ <- operSmsClient.send(
            phone,
            text,
            deliveryStatus => messagesRepository.update(id)(DeliveryStatus.fromApi(deliveryStatus)),
          )
        } yield id
    }
}
