package ptpger.integrations.opersms.domain

import io.circe.generic.JsonCodec
import io.circe.generic.extras.Configuration

import ptpger.integrations.opersms.domain.StatusResponse.SmsStatus

@JsonCodec
case class StatusResponse(
    messages: Option[List[SmsStatus]] = None
  )

object StatusResponse {
  @JsonCodec
  case class SmsStatus(
      `message-id`: Int,
      channel: String,
      status: String,
      `status-date`: String,
    )

  object SmsStatus {
    implicit val configuration: Configuration = Configuration.default.withKebabCaseMemberNames
  }
}
