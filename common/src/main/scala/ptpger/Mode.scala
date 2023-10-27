package ptpger

import enumeratum._

import scala.collection.immutable

sealed trait Mode extends EnumEntry

case object Mode extends Enum[Mode] {
  final case object Development extends Mode
  final case object Test extends Mode
  final case object Production extends Mode
  final case object Staging extends Mode

  lazy val current: Mode =
    sys.env.get("APP_ENV").orElse(sys.props.get("APP_ENV")).map(_.toLowerCase) match {
      case Some("test") => Test
      case Some("staging") | Some("stage") | Some("qa") => Staging
      case Some("prod") | Some("production") => Production
      case _ => Development
    }

  override def values: immutable.IndexedSeq[Mode] = findValues
}
