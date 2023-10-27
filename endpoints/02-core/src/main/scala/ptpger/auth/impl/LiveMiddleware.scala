package ptpger.auth.impl

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.server
import pdi.jwt.JwtAlgorithm
import uz.scala.redis.RedisClient
import uz.scala.syntax.all.circeSyntaxDecoderOps
import uz.scala.syntax.refined.commonSyntaxAutoUnwrapV

import ptpger.auth.AuthConfig
import ptpger.auth.utils.AuthMiddleware
import ptpger.domain.AuthedUser
object LiveMiddleware {
  def make[F[_]: Sync](
      jwtConfig: AuthConfig,
      redis: RedisClient[F],
    ): server.AuthMiddleware[F, AuthedUser] = {
    val userJwtAuth = JwtAuth.hmac(jwtConfig.tokenKey.secret, JwtAlgorithm.HS256)
    def findUser(token: String): F[Option[AuthedUser]] =
      OptionT(redis.get(token))
        .semiflatMap(_.decodeAsF[F, AuthedUser])
        .value

    def destroySession(token: JwtToken): F[Unit] =
      OptionT(findUser(AuthMiddleware.ACCESS_TOKEN_PREFIX + token.value))
        .semiflatMap(user =>
          redis.del(AuthMiddleware.ACCESS_TOKEN_PREFIX + token.value, user.phone)
        )
        .value
        .void

    AuthMiddleware[F, AuthedUser](
      userJwtAuth,
      findUser,
      destroySession,
    )
  }
}
