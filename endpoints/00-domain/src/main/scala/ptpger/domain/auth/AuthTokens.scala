package ptpger.domain.auth

import io.circe.generic.JsonCodec

@JsonCodec
case class AuthTokens(accessToken: String, refreshToken: String)
