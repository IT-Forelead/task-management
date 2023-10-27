package ptpger.auth.impl

import cats.data.EitherT
import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.AuthHeaders
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import dev.profunktor.auth.jwt.JwtToken
import ptpger.Phone
import ptpger.auth.AuthConfig
import ptpger.auth.utils.AuthMiddleware
import ptpger.auth.utils.JwtExpire
import ptpger.auth.utils.Tokens
import ptpger.domain.AuthedUser
import ptpger.domain.auth.AccessCredentials
import ptpger.domain.auth.AuthTokens
import ptpger.domain.auth.Credentials
import ptpger.exception.AError.AuthError
import ptpger.exception.AError.AuthError._
import org.http4s.Request
import org.typelevel.log4cats.Logger
import pdi.jwt.JwtAlgorithm
import tsec.passwordhashers.jca.SCrypt
import uz.scala.redis.RedisClient
import uz.scala.syntax.all.circeSyntaxDecoderOps
import uz.scala.syntax.refined.commonSyntaxAutoRefineV
import uz.scala.syntax.refined.commonSyntaxAutoUnwrapV

trait Auth[F[_], A] {
  def login(credentials: Credentials): F[AuthTokens]
  def destroySession(request: Request[F], phone: Phone): F[Unit]
  def refresh(request: Request[F]): F[AuthTokens]
}

object Auth {
  def make[F[_]: Sync](
      config: AuthConfig,
      findUser: Phone => F[Option[AccessCredentials[AuthedUser]]],
      redis: RedisClient[F],
    )(implicit
      logger: Logger[F]
    ): Auth[F, AuthedUser] =
    new Auth[F, AuthedUser] {
      val tokens: Tokens[F] =
        Tokens.make[F](JwtExpire[F], config)
      val jwtAuth: JwtSymmetricAuth = JwtAuth.hmac(config.tokenKey.secret, JwtAlgorithm.HS256)

      override def login(credentials: Credentials): F[AuthTokens] =
        findUser(credentials.phone).flatMap {
          case None =>
            NoSuchUser("User Not Found").raiseError[F, AuthTokens]
          case Some(person) if !SCrypt.checkpwUnsafe(credentials.password, person.password) =>
            PasswordDoesNotMatch("Password does not match").raiseError[F, AuthTokens]
          case Some(person) =>
            OptionT(redis.get(credentials.phone))
              .cataF(
                createNewToken(person.data),
                json =>
                  for {
                    tokens <- json.decodeAsF[F, AuthTokens]
                    validTokens <- EitherT(
                      AuthMiddleware
                        .validateJwtToken[F](
                          JwtToken(tokens.accessToken),
                          jwtAuth,
                          _ => redis.del(tokens.accessToken, tokens.refreshToken, credentials.phone),
                        )
                    ).foldF(
                      error =>
                        logger.info(s"Tokens recreated reason of that: $error") *>
                          createNewToken(person.data),
                      _ => tokens.pure[F],
                    )
                  } yield validTokens,
              )
        }

      override def refresh(request: Request[F]): F[AuthTokens] = {
        val task = for {
          refreshToken <- EitherT(
            AuthMiddleware
              .getAndValidateJwtToken[F](
                jwtAuth,
                token =>
                  for {
                    _ <- OptionT(redis.get(AuthMiddleware.REFRESH_TOKEN_PREFIX + token))
                      .semiflatMap(_.decodeAsF[F, AuthedUser])
                      .semiflatMap(person => redis.del(person.phone))
                      .value
                    _ <- redis.del(AuthMiddleware.REFRESH_TOKEN_PREFIX + token.value)
                  } yield {},
              )
              .apply(request)
          )
          person <- EitherT
            .fromOptionF(
              redis.get(AuthMiddleware.REFRESH_TOKEN_PREFIX + refreshToken.value),
              "Refresh token expired",
            )
            .semiflatMap(_.decodeAsF[F, AuthedUser])
          _ <- EitherT.right[String](clearOldTokens(person.phone))
          tokens <- EitherT.right[String](createNewToken(person))
        } yield tokens
        task.leftMap(AuthError.InvalidToken.apply).rethrowT
      }

      override def destroySession(request: Request[F], phone: Phone): F[Unit] =
        AuthHeaders
          .getBearerToken(request)
          .traverse_(token => redis.del(AuthMiddleware.ACCESS_TOKEN_PREFIX + token.value, phone))

      private def createNewToken(person: AuthedUser): F[AuthTokens] =
        for {
          tokens <- tokens.createToken[AuthedUser](person)
          accessToken = AuthMiddleware.ACCESS_TOKEN_PREFIX + tokens.accessToken
          refreshToken = AuthMiddleware.REFRESH_TOKEN_PREFIX + tokens.refreshToken
          _ <- redis.put(accessToken, person, config.accessTokenExpiration.value)
          _ <- redis.put(refreshToken, person, config.refreshTokenExpiration.value)
          _ <- redis.put(person.phone, tokens, config.refreshTokenExpiration.value)
        } yield tokens

      private def clearOldTokens(phone: Phone): F[Unit] =
        OptionT(redis.get(phone))
          .semiflatMap(_.decodeAsF[F, AuthTokens])
          .semiflatMap(tokens =>
            redis.del(
              s"${AuthMiddleware.REFRESH_TOKEN_PREFIX}${tokens.refreshToken}",
              s"${AuthMiddleware.ACCESS_TOKEN_PREFIX}${tokens.accessToken}",
            )
          )
          .value
          .void
    }
}
