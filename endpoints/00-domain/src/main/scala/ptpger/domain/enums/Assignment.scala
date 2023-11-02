package ptpger.domain.enums

import enumeratum.EnumEntry.Snakecase
import enumeratum._
import eu.timepit.refined.types.string.NonEmptyString

sealed trait Assignment extends Snakecase {
  def description(firstname: NonEmptyString, lastname: NonEmptyString): String
}
object Assignment extends Enum[Assignment] with CirceEnum[Assignment] {
  case object Assigned extends Assignment {
    override def description(firstname: NonEmptyString, lastname: NonEmptyString): String =
      s"$firstname $lastname foydalanuvchiga vazifa topshirildi"
  }
  case object Unassigned extends Assignment {
    override def description(firstname: NonEmptyString, lastname: NonEmptyString): String =
      s"$firstname $lastname foydalanuvchidagi vazifa bekor qilindi"
  }
  override def values: IndexedSeq[Assignment] = findValues
}
