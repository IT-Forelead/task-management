package uz.scala.sttp

import sttp.model.StatusCode

final case class SttpError(statusCode: StatusCode, message: String)
  extends Exception(s"HttpError $statusCode: $message") {
  override def getMessage: String = s"HttpError $statusCode: $message"
}
