package ptpger.integrations.opersms.domain

import scala.collection.immutable

import enumeratum.EnumEntry.Lowercase
import enumeratum._

sealed trait DeliveryStatus extends EnumEntry with Lowercase

object DeliveryStatus extends CirceEnum[DeliveryStatus] with Enum[DeliveryStatus] {
  case object SENT extends DeliveryStatus
  case object DELIVERED extends DeliveryStatus
  case object FAILED extends DeliveryStatus
  case object UNDEFINED extends DeliveryStatus
  override def values: immutable.IndexedSeq[DeliveryStatus] = findValues
}
