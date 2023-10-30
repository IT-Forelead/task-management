package ptpger.integrations.opersms.domain

import java.time.ZonedDateTime

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class SmsResponse(
    recipient: String,
    text: String,
    dateReceived: ZonedDateTime,
    clientId: String,
    requestId: Int,
    messageId: Int,
    _id: String,
  )

object SmsResponse {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
}
