package ptpger.integrations.opersms

import cats.effect.IO
import cats.effect.Resource
import eu.timepit.refined.pureconfig._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.generic.auto.exportReader
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import weaver.IOSuite
import weaver.scalacheck.Checkers
import ptpger.integrations.opersms.OperSmsClient
import ptpger.integrations.opersms.OperSmsConfig
import ptpger.syntax.refined.commonSyntaxAutoRefineV
import ptpger.utils.ConfigLoader

object OperSmsClientSpec extends IOSuite with Checkers {
  type Res = OperSmsClient[IO]
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  lazy val loadConfig: IO[OperSmsConfig] =
    ConfigLoader.load[IO, OperSmsConfig]

  test("Send sms") { resource =>
    resource
      .send("+998999673398", "TEST", IO.println)
      .as(success)
      .handleErrorWith { error =>
        logger.error(error)("Error occurred while").as(failure("Send sms failed"))
      }

  }

  override def sharedResource: Resource[IO, Res] =
    AsyncHttpClientFs2Backend.resource[IO]().evalMap { implicit backend =>
      loadConfig.map(config => OperSmsClient.make[IO](config))
    }
}
