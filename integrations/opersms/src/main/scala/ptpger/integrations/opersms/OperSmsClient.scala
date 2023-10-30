package ptpger.integrations.opersms

import scala.concurrent.duration.DurationInt

import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.Async
import cats.effect.Sync
import cats.effect.implicits.genSpawnOps
import cats.implicits._
import eu.timepit.refined.types.string.NonEmptyString
import org.typelevel.log4cats.Logger
import retry.RetryPolicies.exponentialBackoff
import retry.RetryPolicies.limitRetries
import retry.RetryPolicy
import uz.scala.sttp.SttpBackends
import uz.scala.sttp.SttpClient
import uz.scala.sttp.SttpClientAuth

import ptpger.exception.AError
import ptpger.integrations.opersms.domain.DeliveryStatus
import ptpger.integrations.opersms.domain.RequestId
import ptpger.integrations.opersms.domain.SMS
import ptpger.integrations.opersms.domain.SmsResponse
import ptpger.integrations.opersms.requests.CheckStatus
import ptpger.integrations.opersms.requests.SendSms
import ptpger.integrations.opersms.retries.Retry
import ptpger.refinements.Phone
import ptpger.syntax.generic.genericSyntaxStringOps

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
        s"""Message sent to [${phone.value.maskMiddlePart(6)}], message text [ \n$text\n ]"""
      )
  }

  private class OperSmsClientImpl[F[_]: Async: SttpBackends.Simple](
      config: OperSmsConfig
    )(implicit
      logger: Logger[F]
    ) extends OperSmsClient[F] {
    private val retryPolicy: RetryPolicy[F] =
      limitRetries[F](10) |+| exponentialBackoff[F](10.seconds)

    private lazy val client: SttpClient.CirceJson[F] = SttpClient.circeJson(
      config.apiURL,
      SttpClientAuth.noAuth,
    )
    private lazy val clientStatus: SttpClient.CirceJson[F] = SttpClient.circeJson(
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
            .delayBy(checkSmsStatus(smsResp, changeStatus), config.checkStatusTime.minutes)
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
      ): F[Unit] = {
      val smsStatus = CheckStatus(
        config.login,
        config.password,
        NonEmptyList.one(RequestId(smsResponse.requestId.toString)),
      )
      val task = clientStatus
        .request(smsStatus)
        .flatMap { response =>
          logger.info(s"======= SMS Status CRAZY RESPONSE: $response ========") *>
            response
              .messages
              .fold(
                AError
                  .MessageError
                  .UnknownSmsStatus("UNKNOWN")
                  .raiseError[F, Unit]
              ) { msg =>
                msg.headOption.fold(Sync[F].unit) { smsStatus =>
                  changeStatus(DeliveryStatus.withName(smsStatus.status.toLowerCase)) *>
                    (if (
                         DeliveryStatus
                           .withName(smsStatus.status.toLowerCase) == DeliveryStatus.UNDEFINED
                     )
                       AError
                         .MessageError
                         .UnknownSmsStatus("UNKNOWN")
                         .raiseError[F, Unit]
                     else
                       logger.info(s"Check SMS Status ${smsStatus.status}"))
                }
              }
        }
        .handleErrorWith { error =>
          logger.error(error)("Error occurred while check sms status")
        }
      Retry[F]
        .retry(retryPolicy)(task)
        .handleErrorWith { error =>
          logger.error(error)("Error occurred where check sms status loop")
        }

    }
  }
}
