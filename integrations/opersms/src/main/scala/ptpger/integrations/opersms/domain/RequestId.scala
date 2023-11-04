package ptpger.integrations.opersms.domain

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class RequestId(requestId: Int)

object RequestId {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
}
