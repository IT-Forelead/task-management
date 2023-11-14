package ptpger.domain.args.messages

import eu.timepit.refined.types.all.PosInt
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class MessageFilter(
    limit: Option[PosInt],
    offset: Option[PosInt],
  )
