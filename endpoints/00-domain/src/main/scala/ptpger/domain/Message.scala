package ptpger.domain

import java.time.ZonedDateTime

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import ptpger.Phone
import ptpger.integrations.opersms.domain.DeliveryStatus

@JsonCodec
case class Message(
    id: MessageId,
    sentAt: ZonedDateTime,
    phone: Phone,
    text: NonEmptyString,
    status: DeliveryStatus,
  )
