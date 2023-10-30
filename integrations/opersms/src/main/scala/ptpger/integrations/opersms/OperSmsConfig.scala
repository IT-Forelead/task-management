package ptpger.integrations.opersms

import java.net.URI

import eu.timepit.refined.types.string.NonEmptyString

case class OperSmsConfig(
    apiURL: URI,
    statusApiURL: URI,
    checkStatusTime: Int,
    login: NonEmptyString,
    password: NonEmptyString,
    enabled: Boolean = false,
  )
