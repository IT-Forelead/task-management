package ptpger.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait Role extends Snakecase
object Role extends Enum[Role] with CirceEnum[Role] {
  case object Admin extends Role
  case object Creator extends Role
  case object Executor extends Role
  case object Controller extends Role
  override def values: IndexedSeq[Role] = findValues
}
