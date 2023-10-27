package ptpger.http

import cats.effect.Async
import ptpger.Algebras
import ptpger.domain.AuthedUser
import org.http4s.server
import uz.scala.http4s.HttpServerConfig
case class Environment[F[_]: Async](
    config: HttpServerConfig,
    middleware: server.AuthMiddleware[F, AuthedUser],
    algebras: Algebras[F],
  )
