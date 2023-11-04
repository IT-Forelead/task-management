package ptpger.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import org.http4s.AuthedRoutes
import org.http4s._
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import ptpger.algebras.UsersAlgebra
import ptpger.domain._
import ptpger.domain.args.users.UserFilters
final case class UsersRoutes[F[_]: JsonDecoder: MonadThrow](
    users: UsersAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/users"

  override val public: HttpRoutes[F] = HttpRoutes.empty[F]

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decodeR[UserFilters] { filters =>
        users.get(filters).flatMap(Ok(_))
      }
  }
}
