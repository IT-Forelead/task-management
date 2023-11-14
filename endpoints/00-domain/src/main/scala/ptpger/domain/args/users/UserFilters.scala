package ptpger.domain
package args.users

import cats.data.NonEmptyList
import eu.timepit.refined.types.numeric.PosInt
import io.circe.generic.JsonCodec
import io.circe.refined._

import ptpger.domain.enums.Role
import ptpger.domain.enums.UserEmployment

@JsonCodec
case class UserFilters(
    id: Option[PersonId] = None,
    employment: Option[UserEmployment] = None,
    roles: Option[NonEmptyList[Role]] = None,
    limit: Option[PosInt] = None,
    offset: Option[PosInt] = None,
  )
