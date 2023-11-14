package ptpger.repos.sql

import shapeless.HNil
import skunk._
import skunk.implicits._

import ptpger.domain.Message
import ptpger.domain.MessageId
import ptpger.domain.enums.DeliveryStatus

private[repos] object MessagesSql extends Sql[MessageId] {
  private[repos]  val codec = (id *: zonedDateTime *: phone *: nes *: deliveryStatus).to[Message]

  val insert: Command[Message] =
    sql"""INSERT INTO messages VALUES ($codec)""".command

  val get: AppliedFragment =
    sql"""SELECT *, COUNT(*) OVER() AS total FROM messages ORDER BY created_at DESC""".apply(Void)

  val update: Command[DeliveryStatus *: MessageId *: EmptyTuple] =
    sql"""UPDATE messages SET status = $deliveryStatus WHERE id = $id"""
      .command
      .contramap {
        case deliveryStatus *: id *: HNil =>
          deliveryStatus *: id *: EmptyTuple
      }
}
