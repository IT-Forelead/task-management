package ptpger

import ptpger.auth.impl.Auth
import ptpger.domain.AuthedUser

case class Algebras[F[_]](
    auth: Auth[F, AuthedUser],
  )
