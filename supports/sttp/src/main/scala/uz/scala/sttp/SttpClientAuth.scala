package uz.scala.sttp

import cats.data.Reader
import cats.kernel.Semigroup
import sttp.client3._

trait SttpClientAuth { self =>
  def reader[T, R]: SttpClientAuth.AuthRequestT[T, R]
  def show: String

  def apply[T, R](req: Request[T, R]): Request[T, R] = reader.run(req)

  def compose(auth: SttpClientAuth): SttpClientAuth =
    new SttpClientAuth {
      def reader[T, R]: SttpClientAuth.AuthRequestT[T, R] =
        auth.reader[T, R].compose(self.reader[T, R])

      def show: String = s"${self.show} | ${auth.show}"
    }
}

object SttpClientAuth {
  type AuthRequestT[T, R] = Reader[Request[T, R], Request[T, R]]

  def noAuth: SttpClientAuth = new SttpClientAuth {
    def reader[T, R]: AuthRequestT[T, R] = Reader(identity)
    def show: String = "no-auth"
  }

  def basic(user: String, password: String): SttpClientAuth = new SttpClientAuth {
    def reader[T, R]: AuthRequestT[T, R] = Reader(_.auth.basic(user, password))
    def show: String = s"basic($user)"
  }

  def bearer(token: String): SttpClientAuth = new SttpClientAuth {
    def reader[T, R]: AuthRequestT[T, R] = Reader(_.auth.bearer(token))
    def show: String = s"bearer(${mask(token)})"
  }

  def withHeaders(headers: (String, String)*): SttpClientAuth = new SttpClientAuth {
    def reader[T, R]: AuthRequestT[T, R] = Reader(_.headers(headers.toMap))
    def show: String = {
      val q = headers
        .map { case (k, v) => k + ":" + mask(v) }
        .reduceOption(_ + ";" + _)
        .getOrElse("")
      s"withHeaders($q)"
    }
  }
  def withUriParams(params: (String, String)*): SttpClientAuth = new SttpClientAuth {
    def reader[T, R]: AuthRequestT[T, R] = Reader { req =>
      req.method(req.method, req.uri.addParams(params: _*))
    }

    def show: String = {
      val q = params
        .map { case (k, v) => k + "=" + mask(v) }
        .reduceOption(_ + "&" + _)
        .getOrElse("")
      s"withUriParams($q)"
    }
  }

  implicit def semigroup: Semigroup[SttpClientAuth] =
    (x: SttpClientAuth, y: SttpClientAuth) => y.compose(x)

  private def mask(s: String): String =
    s.patch(4, "*" * (s.length - 4), s.length)
}
