package ptpger.domain.args

import java.time.ZonedDateTime

import io.circe.generic.JsonCodec

@JsonCodec
case class DateTimeRange(from: Option[ZonedDateTime] = None, to: Option[ZonedDateTime] = None)
