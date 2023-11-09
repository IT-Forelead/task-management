package ptpger.domain
package args.tasks

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec
import io.circe.refined._

@JsonCodec
case class CommentInput(
    taskId: TaskId,
    note: NonEmptyString,
    assetId: Option[AssetId],
  )
