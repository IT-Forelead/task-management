package ptpger.repos.sql

import shapeless.HNil
import skunk._
import skunk.implicits._

import ptpger.domain.Message
import ptpger.domain.MessageId
import ptpger.domain.enums.DeliveryStatus

private[repos] object MessagesSql extends Sql[MessageId] {
  private val codec = (id *: zonedDateTime *: phone *: nes *: deliveryStatus).to[Message]

  val insert: Command[Message] =
    sql"""INSERT INTO messages VALUES ($codec)""".command

  val get: Query[Void, Message] =
    sql"""SELECT * FROM messages ORDER BY created_at DESC""".query(codec)

  val update: Command[DeliveryStatus *: MessageId *: EmptyTuple] =
    sql"""UPDATE messages SET status = $deliveryStatus WHERE id = $id"""
      .command
      .contramap {
        case deliveryStatus *: id *: HNil =>
          deliveryStatus *: id *: EmptyTuple
      }
}
