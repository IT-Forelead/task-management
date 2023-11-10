package ptpger.integrations.opersms

import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.Async
import cats.effect.Sync
import cats.effect.implicits.genSpawnOps
import cats.effect.implicits.genTemporalOps_
import cats.implicits._
import eu.timepit.refined.types.string.NonEmptyString
import org.typelevel.log4cats.Logger
import uz.scala.sttp.CirceJsonResponse
import uz.scala.sttp.SttpBackends
import uz.scala.sttp.SttpClient
import uz.scala.sttp.SttpClientAuth
import uz.scala.syntax.generic.genericSyntaxStringOps

import ptpger.Phone
import ptpger.integrations.opersms.domain.DeliveryStatus
import ptpger.integrations.opersms.domain.RequestId
import ptpger.integrations.opersms.domain.SMS
import ptpger.integrations.opersms.domain.SmsResponse
import ptpger.integrations.opersms.requests.CheckStatus
import ptpger.integrations.opersms.requests.SendSms

trait OperSmsClient[F[_]] {
  def send(
      phone: Phone,
      text: NonEmptyString,
      changeStatus: DeliveryStatus => F[Unit],
    ): F[Unit]
}

object OperSmsClient {
  def make[F[_]: Async: SttpBackends.Simple: Logger](
      config: OperSmsConfig
    ): OperSmsClient[F] =
    if (config.enabled)
      new OperSmsClientImpl[F](config)
    else
      new NoOpOperSmsClientImpl[F]

  private class NoOpOperSmsClientImpl[F[_]: Logger] extends OperSmsClient[F] {
    override def send(
        phone: Phone,
        text: NonEmptyString,
        changeStatus: DeliveryStatus => F[Unit],
      ): F[Unit] =
      Logger[F].info(
        s"""Sms sent to [${phone.value.maskMiddlePart(6)}], sms text [ \n$text\n ]"""
      )
  }

  private class OperSmsClientImpl[F[_]: Async: SttpBackends.Simple](
      config: OperSmsConfig
    )(implicit
      logger: Logger[F]
    ) extends OperSmsClient[F] {
    private lazy val client: SttpClient[F, CirceJsonResponse] = SttpClient.circeJson(
      config.apiURL,
      SttpClientAuth.noAuth,
    )
    private lazy val clientStatus: SttpClient[F, CirceJsonResponse] = SttpClient.circeJson(
      config.statusApiURL,
      SttpClientAuth.noAuth,
    )

    override def send(
        phone: Phone,
        text: NonEmptyString,
        changeStatus: DeliveryStatus => F[Unit],
      ): F[Unit] = {
      val smsTest = for {
        _ <- logger.info(s"Start sending sms to [${phone.value.maskMiddlePart(6)}]")
        sendSMS = SendSms(
          config.login,
          config.password,
          NonEmptyList.one(SMS.unPlus(phone, text)),
        )
        sms <- OptionT(client.request(sendSMS).map(_.headOption))
          .semiflatTap(smsResponse =>
            logger.info(
              s"OperSms response recipient: [${smsResponse.recipient.maskMiddlePart(6)}], request_id: [${smsResponse.requestId}]"
            )
          )
          .value
        _ <- sms.fold(Sync[F].unit)(smsResp =>
          Sync[F]
            .delayBy(checkSmsStatus(smsResp, changeStatus), config.checkStatusTime)
            .start
            .void
        )
      } yield ()
      smsTest.handleErrorWith { error =>
        logger.error(error)("Error occurred while send sms")
      }
    }

    private def checkSmsStatus(
        smsResponse: SmsResponse,
        changeStatus: DeliveryStatus => F[Unit],
        attempt: Int = 1,
      ): F[Unit] = {
      val smsStatus = CheckStatus(
        config.login,
        config.password,
        NonEmptyList.one(RequestId(smsResponse.requestId)),
      )
      for {
        _ <- logger.info(s"Request to check sms status [${smsResponse.requestId}]")
        _ <-
          OptionT(clientStatus.request(smsStatus).map(_.messages.headOption))
            .semiflatTap(smsStatus => changeStatus(smsStatus.status))
            .cataF(
              Sync[F].unit,
              statusSmsResponse =>
                checkSmsStatus(smsResponse, changeStatus, attempt + 1)
                  .delayBy(config.checkStatusTime)
                  .whenA(statusSmsResponse.status == DeliveryStatus.Undefined && attempt <= 3) >>
                  logger.info(s"Check SMS Status ${statusSmsResponse.status}"),
            )
      } yield ()
    }
  }
}
