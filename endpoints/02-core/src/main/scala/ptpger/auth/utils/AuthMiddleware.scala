package ptpger.auth.utils

import cats.data.EitherT
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all._
import dev.profunktor.auth.jwt._
import org.http4s.Credentials.Token
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server
import pdi.jwt._

object AuthMiddleware {
  val ACCESS_TOKEN_PREFIX = "ACCESS_"
  val REFRESH_TOKEN_PREFIX = "REFRESH_"
  def getBearerToken[F[_]: Sync]: Kleisli[F, Request[F], Option[JwtToken]] =
    Kleisli { request =>
      Sync[F].delay(
        request
          .headers
          .get[Authorization]
          .collect {
            case Authorization(Token(AuthScheme.Bearer, token)) => JwtToken(token)
          }
          .orElse {
            request.params.get("X-Access-Token").map(JwtToken.apply)
          }
      )
    }

  def validateJwtToken[F[_]: Sync](
      token: JwtToken,
      jwtAuth: JwtSymmetricAuth,
      removeToken: JwtToken => F[Unit],
    ): F[Either[String, JwtToken]] =
    Jwt
      .decode(
        token.value,
        jwtAuth.secretKey.value,
        jwtAuth.jwtAlgorithms,
      )
      .liftTo
      .map(_ => token.asRight[String])
      .handleErrorWith { _ =>
        removeToken(token).as {
          "Invalid token".asLeft[JwtToken]
        }
      }
  def getAndValidateJwtToken[F[_]: Sync](
      jwtAuth: JwtSymmetricAuth,
      removeToken: JwtToken => F[Unit],
    ): Kleisli[F, Request[F], Either[String, JwtToken]] =
    Kleisli { request =>
      EitherT
        .fromOptionF(getBearerToken[F].apply(request), "Bearer token not found")
        .flatMapF { token =>
          validateJwtToken(token, jwtAuth, removeToken)
        }
        .value
    }

  def apply[F[_]: Sync, A](
      jwtAuth: JwtSymmetricAuth,
      authenticate: String => F[Option[A]],
      removeToken: JwtToken => F[Unit],
    ): server.AuthMiddleware[F, A] = { routes: AuthedRoutes[A, F] =>
    val dsl = new Http4sDsl[F] {}; import dsl._

    val onFailure: AuthedRoutes[String, F] =
      Kleisli(req => OptionT.liftF(Forbidden(req.context)))

    def getUser(
        token: JwtToken
      ): EitherT[F, String, A] =
      EitherT.fromOptionF(authenticate(ACCESS_TOKEN_PREFIX + token.value), "Access token expired")

    Kleisli { (req: Request[F]) =>
      OptionT {
        EitherT(getAndValidateJwtToken[F](jwtAuth, removeToken).apply(req))
          .flatMap(getUser)
          .foldF(
            err => onFailure(AuthedRequest(err, req)).value,
            user => routes(AuthedRequest(user, req)).value,
          )
      }
    }
  }
}
