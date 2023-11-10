package ptpger.domain
package args.users

import cats.data.NonEmptyList
import io.circe.generic.JsonCodec

import ptpger.domain.enums.Role
import ptpger.domain.enums.UserEmployment

@JsonCodec
case class UserFilters(
    id: Option[PersonId] = None,
    employment: Option[UserEmployment] = None,
    roles: Option[NonEmptyList[Role]] = None,
  )
