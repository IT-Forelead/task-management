package uz.scala.http4s

import cats.MonadThrow
import cats.implicits.catsSyntaxFlatMapOps
import org.http4s.HttpRoutes
import org.http4s.MalformedMessageBodyFailure
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger
import uz.scala.http4s.utils.HttpErrorHandler
import ptpger.exception.AError
import ptpger.exception.AError.AuthError
import uz.scala.MultipartDecodeError
class LiveHttpErrorHandler[F[_]: MonadThrow](
    implicit
    logger: Logger[F]
  ) extends HttpErrorHandler[F, AError]
       with Http4sDsl[F] {
  private val handler: PartialFunction[Throwable, F[Response[F]]] = {
    case AuthError.PasswordDoesNotMatch(message) =>
      logger.info(message) >>
        Forbidden("Incorrect Login or password")
    case AuthError.NoSuchUser(message) =>
      logger.info(message) >>
        Forbidden("Incorrect Login or password")
    case AuthError.InvalidToken(message) =>
      logger.info(message) >>
        Forbidden(message)
    case error: AError =>
      logger.info(error)("Error occurred") >>
        BadRequest(error.cause)
    case error: MalformedMessageBodyFailure =>
      logger.info(error)("Invalid json entered") >>
        UnprocessableEntity(error.details)
    case error: MultipartDecodeError =>
      logger.info(error)("Invalid form data entered") >>
        UnprocessableEntity(error.cause)
    case error: IllegalArgumentException =>
      logger.info(error)("Incorrect argument entered") >>
        UnprocessableEntity(error.getMessage.replace("requirement failed: ", ""))
    case throwable =>
      logger.error(throwable)("Error occurred while processing request") >>
        BadRequest("Something went wrong. Please try again in a few minutes")
  }

  override def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
    RoutesHttpErrorHandler(routes)(handler)
}
