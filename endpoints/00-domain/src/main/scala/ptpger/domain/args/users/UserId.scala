package ptpger.domain
package args.users

import io.circe.generic.JsonCodec

@JsonCodec
case class UserId(
    id: PersonId
  )
