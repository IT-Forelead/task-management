package ptpger.domain
package auth

import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt


case class AccessCredentials[+U](data: U, password: PasswordHash[SCrypt])
object AccessCredentials {
  implicit def encoder[U: Encoder]: Encoder.AsObject[AccessCredentials[U]] =
    deriveEncoder[AccessCredentials[U]]
  implicit def decoder[U: Decoder]: Decoder[AccessCredentials[U]] =
    deriveDecoder[AccessCredentials[U]]
}
