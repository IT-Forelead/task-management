package ptpger.domain

import io.circe.generic.JsonCodec

@JsonCodec
case class Counts (
    count: Long,
    `new`: Long,
    inProgress: Long,
    completed: Long,
    onHold: Long,
    rejected: Long,
    approved: Long,
    expired: Long,
)
