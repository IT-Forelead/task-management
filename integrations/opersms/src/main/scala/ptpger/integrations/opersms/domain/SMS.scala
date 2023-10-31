package ptpger.integrations.opersms.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec

import ptpger.Phone
import uz.scala.syntax.refined.commonSyntaxAutoUnwrapV

@JsonCodec
case class SMS(
    phone: String,
    text: String,
  )

object SMS {
  def unPlus(phone: Phone, text: NonEmptyString): SMS =
    SMS(phone.value.replace("+", ""), text)
}
