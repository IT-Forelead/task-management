package ptpger.domain

import io.circe.generic.JsonCodec

@JsonCodec
case class CountsAll (
    firstname: String,
    lastname: String,
    count: Long,
    `new`: Long,
    inProgress: Long,
    completed: Long,
    onHold: Long,
    rejected: Long,
    approved: Long,
    expired: Long
)
