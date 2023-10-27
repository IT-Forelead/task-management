package uz.scala.http4s

import eu.timepit.refined.types.net.NonSystemPortNumber

final case class HttpServerConfig(
    port: NonSystemPortNumber,
    logger: LogConfig,
  )
