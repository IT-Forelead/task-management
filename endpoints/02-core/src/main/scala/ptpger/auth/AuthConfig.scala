package ptpger.auth

import ptpger.domain.JwtAccessTokenKey
import ptpger.domain.TokenExpiration

case class AuthConfig(
    tokenKey: JwtAccessTokenKey,
    accessTokenExpiration: TokenExpiration,
    refreshTokenExpiration: TokenExpiration,
  )
