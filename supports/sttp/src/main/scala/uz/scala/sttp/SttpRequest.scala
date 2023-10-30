package uz.scala.sttp

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import scala.concurrent.duration.Duration

import io.circe.Encoder
import io.circe.syntax._
import sttp.model._

trait SttpRequest[Req, *] {
  type Path = Req => String
  type Params = Req => Map[String, String]
  type Headers = Req => Map[String, String]
  type Body = Req => SttpRequestBody

  def method: Method
  def timeout: Duration = sttp.client3.DefaultReadTimeout

  def path: Path
  def noPath: Path = _ => ""

  def params: Params = _ => Map.empty
  def headers: Headers = _ => Map.empty
  def uri: Req => String = r => getUri(params(r))(r)

  def getUri(params: Map[String, String]): Req => String = r =>
    if (queryString(params).trim.nonEmpty)
      s"${path(r)}?${queryString(params)}"
    else
      path(r)

  def queryString(params: Map[String, String]): String =
    params
      .map {
        case (k, v) =>
          val key = URLEncoder.encode(k, StandardCharsets.UTF_8.name())
          val value = URLEncoder.encode(v, StandardCharsets.UTF_8.name())

          s"$key=$value"
      }
      .mkString("&")

  def body: Body

  def noBody: Body = _ => SttpRequestBody.SttpRequestNoBody

  def jsonBody(implicit encoder: Encoder[Req]): Body =
    r => SttpRequestBody.SttpRequestJsonBody(r.asJson.deepDropNullValues)

  def jsonBodyFrom[Data: Encoder](req: Req => Data): Body =
    r => SttpRequestBody.SttpRequestJsonBody(req(r).asJson.deepDropNullValues)

  def formBody(f: Req => Map[String, String]): Body =
    r => SttpRequestBody.SttpRequestFormBody(f(r))
}

object SttpRequest {
  type Aux[Req, Res] = SttpRequest[Req, Res]
}
