package uz.scala.http4s

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

class HealthHttpRoutes[F[_]: Monad] extends Http4sDsl[F] {
  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "status" =>
        Ok("OK")
    }
}
