package uz.scala

final case class MultipartDecodeError(cause: String) extends Throwable {
  override def getMessage: String = cause
}
