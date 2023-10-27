package uz.scala.http4s

import cats.MonadThrow
import cats.data.Kleisli
import cats.data.OptionT
import cats.implicits.catsSyntaxApplicativeError
import cats.implicits.toFunctorOps
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Response

object RoutesHttpErrorHandler {
  def apply[F[_]: MonadThrow](
      routes: HttpRoutes[F]
    )(
      handler: PartialFunction[Throwable, F[Response[F]]]
    ): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      OptionT {
        routes
          .run(req)
          .value
          .handleErrorWith(e => handler(e).map(Option(_)))
      }
    }
}
