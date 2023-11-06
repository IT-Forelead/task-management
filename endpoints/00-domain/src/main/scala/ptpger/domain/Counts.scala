package ptpger.domain

import io.circe.generic.JsonCodec

@JsonCodec
case class Counts (
    totalCount: Int,
    `new`: Int,
    inProgress: Int,
    complete: Int,
    onHold: Int,
)
