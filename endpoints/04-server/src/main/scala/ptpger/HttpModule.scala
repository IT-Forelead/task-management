package ptpger

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.ExitCode
import cats.effect.kernel.Concurrent
import cats.effect.kernel.Resource
import cats.implicits.toFunctorOps
import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import uz.scala.http4s.HttpServer
import uz.scala.http4s.utils.Routes

import ptpger.domain.AuthedUser
import ptpger.http.Environment
import ptpger.routes._

object HttpModule {
  private def allRoutes[
      F[_]: MonadThrow: Concurrent: JsonDecoder: Logger: Lambda[M[_] => fs2.Compiler[M, M]]
    ](
      env: Environment[F]
    ): NonEmptyList[HttpRoutes[F]] =
    NonEmptyList
      .of[Routes[F, AuthedUser]](
        new AuthRoutes[F](env.algebras.auth),
        new TasksRoutes[F](env.algebras.tasks),
        new RootRoutes[F](env.algebras.assets),
        new UsersRoutes[F](env.algebras.users),
      )
      .map { r =>
        Router(
          r.path -> (r.public <+> env.middleware(r.`private`))
        )
      }

  def make[F[_]: Async](
      env: Environment[F]
    )(implicit
      logger: Logger[F]
    ): Resource[F, F[ExitCode]] =
    HttpServer.make[F](env.config, _ => allRoutes[F](env)).map { _ =>
      logger.info(s"HTTP server is started").as(ExitCode.Success)
    }
}
