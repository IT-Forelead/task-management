package ptpger.domain

import io.circe.generic.JsonCodec

@JsonCodec
case class Counts (
    count: Int,
    `new`: Int,
    inProgress: Int,
    completed: Int,
    onHold: Int,
    rejected: Int,
    approved: Int,
)
