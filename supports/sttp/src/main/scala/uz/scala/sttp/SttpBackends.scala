package uz.scala.sttp

import sttp.client3.SttpBackend

object SttpBackends {
  type Simple[F[_]] = SttpBackend[F, Any]

  object Simple {
    def apply[F[_]](implicit backend: SttpBackend[F, Any]): SttpBackend[F, Any] = backend
  }
}
