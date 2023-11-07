package ptpger.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait TaskStatus extends Snakecase
object TaskStatus extends Enum[TaskStatus] with CirceEnum[TaskStatus] {
  case object New extends TaskStatus
  case object InProgress extends TaskStatus
  case object Complete extends TaskStatus
  case object OnHold extends TaskStatus
  case object Rejected extends TaskStatus
  case object Approved extends TaskStatus
  override def values: IndexedSeq[TaskStatus] = findValues
}
