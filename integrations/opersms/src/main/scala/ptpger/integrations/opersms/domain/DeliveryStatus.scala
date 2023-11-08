package ptpger.integrations.opersms.domain

import scala.collection.immutable

import enumeratum._

sealed trait DeliveryStatus extends EnumEntry

object DeliveryStatus extends CirceEnum[DeliveryStatus] with Enum[DeliveryStatus] {
  case object Sent extends DeliveryStatus
  case object Delivered extends DeliveryStatus
  case object NotDelivered extends DeliveryStatus
  case object Failed extends DeliveryStatus
  case object Transmitted extends DeliveryStatus
  case object Undefined extends DeliveryStatus
  override def values: immutable.IndexedSeq[DeliveryStatus] = findValues
}
