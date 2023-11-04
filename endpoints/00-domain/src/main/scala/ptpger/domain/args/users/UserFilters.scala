package ptpger.domain
package args.users

import io.circe.generic.JsonCodec

import ptpger.domain.enums.UserEmployment

@JsonCodec
case class UserFilters(
    id: Option[PersonId] = None,
    employment: Option[UserEmployment] = None,
  )
