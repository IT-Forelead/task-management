package ptpger.auth.utils

import cats.Monad
import cats.implicits._
import dev.profunktor.auth.jwt.JwtSecretKey
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.auth.jwt.jwtEncode
import ptpger.auth.AuthConfig
import ptpger.domain.auth.AuthTokens
import ptpger.effects.GenUUID
import io.circe.Encoder
import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtClaim
import uz.scala.syntax.all.genericSyntaxGenericTypeOps
import uz.scala.syntax.refined.commonSyntaxAutoUnwrapV

trait Tokens[F[_]] {
  def createToken[U: Encoder](data: U): F[AuthTokens]
}

object Tokens {
  def make[F[_]: GenUUID: Monad](
      jwtExpire: JwtExpire[F],
      config: AuthConfig,
    ): Tokens[F] =
    new Tokens[F] {
      private def encodeToken: JwtClaim => F[JwtToken] =
        jwtEncode[F](_, JwtSecretKey(config.tokenKey.secret), JwtAlgorithm.HS256)

      override def createToken[U: Encoder](data: U): F[AuthTokens] =
        for {
          accessTokenClaim <- jwtExpire.expiresIn(
            JwtClaim(data.toJson),
            config.accessTokenExpiration,
          )
          accessToken <- encodeToken(accessTokenClaim)
          refreshTokenClaim <- jwtExpire.expiresIn(JwtClaim(), config.refreshTokenExpiration)
          refreshToken <- encodeToken(refreshTokenClaim)
        } yield AuthTokens(accessToken.value, refreshToken.value)
    }
}
