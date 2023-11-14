package ptpger.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s.AuthedRoutes
import org.http4s._
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import ptpger.algebras.MessagesAlgebra
import ptpger.domain._
import ptpger.domain.args.messages.MessageFilter
final case class MessagesRoutes[F[_]: JsonDecoder: MonadThrow](
    messages: MessagesAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/messages"

  override val public: HttpRoutes[F] = HttpRoutes.empty[F]

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decodeR[MessageFilter] { filters =>
        messages.get(filters).flatMap(Ok(_))
      }
  }
}
