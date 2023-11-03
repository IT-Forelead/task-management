package ptpger.http

import cats.effect.Async
import org.http4s.server
import uz.scala.http4s.HttpServerConfig

import ptpger.Algebras
import ptpger.domain.AuthedUser
case class Environment[F[_]: Async](
    config: HttpServerConfig,
    middleware: server.AuthMiddleware[F, AuthedUser],
    algebras: Algebras[F],
  )
