package ptpger.integrations.opersms

import java.net.URI

import scala.concurrent.duration.FiniteDuration

import eu.timepit.refined.types.string.NonEmptyString

case class OperSmsConfig(
    apiURL: URI,
    statusApiURL: URI,
    checkStatusTime: FiniteDuration,
    login: NonEmptyString,
    password: NonEmptyString,
    enabled: Boolean = false,
  )
