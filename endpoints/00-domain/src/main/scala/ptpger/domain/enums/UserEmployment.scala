package ptpger.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._
import eu.timepit.refined.types.string.NonEmptyString

sealed trait UserEmployment extends Snakecase
object UserEmployment extends Enum[UserEmployment] with CirceEnum[UserEmployment] {
  case object Employed extends UserEmployment
  case object Unemployed extends UserEmployment
  override def values: IndexedSeq[UserEmployment] = findValues
}
