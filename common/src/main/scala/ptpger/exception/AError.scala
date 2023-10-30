package ptpger.exception

import java.util.UUID

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.syntax.EncoderOps

@ConfiguredJsonCodec
sealed trait AError extends Throwable {
  def cause: String
  override def getMessage: String = s"${AError.Prefix}${(this: AError).asJson}"
}

object AError {
  val Prefix: String = "AError: "
  implicit val config: Configuration = Configuration.default.withDiscriminator("Kind")

  sealed trait AuthError extends AError
  object AuthError {
    final case class NoSuchUser(cause: String) extends AuthError
    final case class InvalidToken(cause: String) extends AuthError
    final case class AuthenticationException(cause: String) extends AuthError
    final case class AuthorizationException(cause: String) extends AuthError
    final case class PasswordDoesNotMatch(cause: String) extends AuthError
  }

  sealed trait MessageError extends AError
  object MessageError {
    final case class UnknownSmsStatus(status: String) extends MessageError {
      override def cause: String = s"Sms delivery status: $status"
    }
  }
}
