package ptpger.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait Action extends Snakecase
object Action extends Enum[Action] with CirceEnum[Action] {
  case object Assignment extends Action
  case object ChangeStatus extends Action
  case object Note extends Action
  override def values: IndexedSeq[Action] = findValues
}
