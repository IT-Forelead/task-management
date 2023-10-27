package ptpger.auth.utils

import cats.effect.Sync
import cats.implicits.toFunctorOps
import ptpger.domain.TokenExpiration
import pdi.jwt.JwtClaim

trait JwtExpire[F[_]] {
  def expiresIn(claim: JwtClaim, exp: TokenExpiration): F[JwtClaim]
  def isExpired(claim: JwtClaim): F[Boolean]
}

object JwtExpire {
  def apply[F[_]: Sync]: JwtExpire[F] =
    new JwtExpire[F] {
      override def expiresIn(claim: JwtClaim, exp: TokenExpiration): F[JwtClaim] =
        JwtClock[F].utc.map { implicit jClock =>
          claim.issuedNow.expiresIn(exp.value.toSeconds)
        }

      override def isExpired(claim: JwtClaim): F[Boolean] =
        JwtClock[F].utc.map { implicit jClock =>
          !claim.isValid
        }
    }
}
