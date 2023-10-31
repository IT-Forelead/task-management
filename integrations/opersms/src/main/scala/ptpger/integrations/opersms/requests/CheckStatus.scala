package ptpger.integrations.opersms.requests

import cats.data.NonEmptyList
import eu.timepit.refined.types.string.NonEmptyString
import sttp.model.Method

import ptpger.integrations.opersms.domain.RequestId
import ptpger.integrations.opersms.domain.StatusResponse
import uz.scala.sttp.SttpRequest
import uz.scala.syntax.generic.genericSyntaxGenericTypeOps
import uz.scala.syntax.refined.commonSyntaxAutoUnwrapV

case class CheckStatus(
    login: NonEmptyString,
    password: NonEmptyString,
    ids: NonEmptyList[RequestId],
  )

object CheckStatus {
  implicit val sttpRequest: SttpRequest[CheckStatus, StatusResponse] =
    new SttpRequest[CheckStatus, StatusResponse] {
      val method: Method = Method.POST
      val path: Path = _ => "/status"
      def body: Body = formBody { req =>
        Map(
          "login" -> req.login,
          "password" -> req.password.value,
          "data" -> req.ids.toJson,
        )
      }
    }
}
