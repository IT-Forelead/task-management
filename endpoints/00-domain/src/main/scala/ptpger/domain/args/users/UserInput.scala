package ptpger.domain.args.users

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

import ptpger.Phone

@JsonCodec
case class UserInput(
    firstname: NonEmptyString,
    lastname: NonEmptyString,
    phone: Phone,
  )
