package uz.scala.sttp

import java.net.URI

import cats.Show
import cats.effect.Sync
import cats.implicits._
import io.circe.Decoder
import io.circe.Json
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.client3._
import sttp.client3.circe._
import sttp.model._
import uz.scala.sttp.SttpRequestBody._

/** Wrapper around SttpBackend, that has built-in auth mechanisms and support for streaming of paginated responses
  *
  * @tparam F Effect of the request execution
  * @tparam H Http response type, can be JSON/String/XML, whatever else..
  */
trait SttpClient[F[_], H] {
  def request[Req, Res](
      query: Req
    )(implicit
      request: SttpRequest.Aux[Req, Res],
      exec: SttpResponseDecoder[F, H, Res],
    ): F[Res]
}

object SttpClient {
  def apply[F[_]: Sync: SttpBackends.Simple, H](
      apiUrl: URI,
      auth: SttpClientAuth,
      responseAs: ResponseAs[H, Any],
      modifyRequestBodyForShow: String => String = identity[String],
    ): SttpClient[F, H] =
    new SttpClient[F, H] {
      private val logger: Logger[F] = Slf4jLogger.getLoggerFromName[F]("workout.sttp-client")

      override def request[R, Q](
          query: R
        )(implicit
          request: SttpRequest.Aux[R, Q],
          exec: SttpResponseDecoder[F, H, Q],
        ): F[Q] =
        requestWithUri(query, request.uri(query))

      private def requestWithUri[R, Q](
          query: R,
          uriText: String,
        )(implicit
          request: SttpRequest.Aux[R, Q],
          exec: SttpResponseDecoder[F, H, Q],
        ): F[Q] = {

        val uri = Uri(URI.create(s"${apiUrl.toString}$uriText"))

        val response =
          basicRequest
            .headers(request.headers(query))
            .method(request.method, uri)
            .readTimeout(request.timeout)
            .response(responseAs)

        val responseWithAuth = auth(response)

        val reqBody = request.body(query)
        val responseTask = reqBody match {
          case SttpRequestNoBody => responseWithAuth
          case SttpRequestStringBody(body) => responseWithAuth.body(body)
          case SttpRequestJsonBody(body) => responseWithAuth.body(body)
          case SttpRequestFormBody(body) => responseWithAuth.body(body)
        }

        for {
          resp <- implicitly[SttpBackend[F, Any]].send(responseTask)

          responseHeaders = resp
            .headers
            .map(_.toString())
            .reduceOption(_ + "; " + _)
            .getOrElse("")

          requestHeaders = request
            .headers(query)
            .map(_.toString())
            .reduceOption(_ + "; " + _)
            .getOrElse("")

          _ <- logger.info(
            "HTTP Request\n" +
              s"${request.method.toString} $uri\n" +
              " - REQUEST\n" +
              s" -- Auth: ${auth.show}\n" +
              s" -- Header: $requestHeaders\n" +
              s" -- Body: ${modifyRequestBodyForShow(reqBody.show)}\n" +
              " - RESPONSE\n" +
              s" -- ${resp.code} - ${resp.statusText}\n" +
              s" -- Headers: $responseHeaders\n" +
              s" -- Body: ${resp.body}"
          )
          decodedResponse <- exec(resp)
        } yield decodedResponse

      }
    }

  type CirceJson[F[_]] = SttpClient[F, CirceJsonResponse]
  type CirceJsonM[M[_], F[_]] = SttpClient[F, CirceJsonResponseM[M]]

  def circeJson[F[_]: Sync: SttpBackends.Simple](
      apiUrl: URI,
      auth: SttpClientAuth,
    ): SttpClient.CirceJson[F] =
    apply(apiUrl, auth, asJson[Json])

  def circeJsonM[M[_], F[_]: Sync: SttpBackends.Simple](
      apiUrl: URI,
      auth: SttpClientAuth,
    )(implicit
      dec: Decoder[M[Json]],
      iosOpt: IsOption[M[Json]],
      show: Show[M[Json]],
    ): SttpClient.CirceJsonM[M, F] =
    apply(apiUrl, auth, asJson[M[Json]])
}
