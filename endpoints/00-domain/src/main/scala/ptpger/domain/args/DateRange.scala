package ptpger.domain.args

import java.time.LocalDate

import io.circe.generic.JsonCodec

@JsonCodec
case class DateRange(from: Option[LocalDate] = None, to: Option[LocalDate] = None)
