package ptpger.domain.enums

import scala.collection.immutable

import enumeratum.EnumEntry.Snakecase
import enumeratum._

import ptpger.integrations.opersms.domain.{ DeliveryStatus => ApiDeliveryStatus }
sealed trait DeliveryStatus extends Snakecase

object DeliveryStatus extends CirceEnum[DeliveryStatus] with Enum[DeliveryStatus] {
  case object Sent extends DeliveryStatus
  case object Delivered extends DeliveryStatus
  case object NotDelivered extends DeliveryStatus
  case object Failed extends DeliveryStatus
  case object Transmitted extends DeliveryStatus
  case object Undefined extends DeliveryStatus
  override def values: immutable.IndexedSeq[DeliveryStatus] = findValues
  def fromApi: ApiDeliveryStatus => DeliveryStatus = {
    case ApiDeliveryStatus.Sent => Sent
    case ApiDeliveryStatus.Delivered => Delivered
    case ApiDeliveryStatus.NotDelivered => NotDelivered
    case ApiDeliveryStatus.Failed => Failed
    case ApiDeliveryStatus.Transmitted => Transmitted
    case ApiDeliveryStatus.Undefined => Undefined
  }
}
