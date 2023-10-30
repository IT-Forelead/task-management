package ptpger.integrations.opersms.requests

import cats.data.NonEmptyList
import eu.timepit.refined.types.string.NonEmptyString
import sttp.model.HeaderNames
import sttp.model.Method
import uz.scala.sttp.SttpRequest

import ptpger.integrations.opersms.domain.SMS
import ptpger.integrations.opersms.domain.SmsResponse
import ptpger.syntax.generic.genericSyntaxGenericTypeOps
import ptpger.syntax.refined.commonSyntaxAutoUnwrapV

case class SendSms(
    login: NonEmptyString,
    password: NonEmptyString,
    sms: NonEmptyList[SMS],
  )

object SendSms {
  implicit val sttpRequest: SttpRequest[SendSms, List[SmsResponse]] =
    new SttpRequest[SendSms, List[SmsResponse]] {
      val method: Method = Method.POST
      val path: Path = noPath
      override def headers: Headers = _ =>
        Map(
          HeaderNames.ContentType -> "application/x-www-form-urlencoded"
        )
      def body: Body = formBody { req =>
        Map(
          "login" -> req.login,
          "password" -> req.password.value,
          "data" -> req.sms.toJson,
        )
      }
    }
}
