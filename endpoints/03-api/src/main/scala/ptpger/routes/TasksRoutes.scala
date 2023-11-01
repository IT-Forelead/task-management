package ptpger.routes

import cats.Monad
import cats.MonadThrow
import cats.implicits.catsSyntaxFlatMapOps
import cats.implicits.toFlatMapOps
import io.estatico.newtype.ops._
import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.typelevel.log4cats.Logger
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import ptpger.algebras.CommentAlgebra
import ptpger.algebras.TaskAlgebra
import ptpger.domain._
final case class TasksRoutes[F[_]: Monad: JsonDecoder: MonadThrow](
    tasks: TaskAlgebra[F],
    comments: CommentAlgebra[F],
  )(implicit
    logger: Logger[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/tasks"

  override val public: HttpRoutes[F] = HttpRoutes.empty[F]

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decodeR[TaskInput] { taskInput =>
        tasks.create(taskInput).flatMap(Created(_))
      }
    case GET -> Root as _ =>
      tasks.get.flatMap(Ok(_))
    case ar @ PUT -> Root / UUIDVar(id) as _ =>
      ar.req.decodeR[TaskUpdateInput] { taskInput =>
        tasks.update(id.coerce[TaskId], taskInput) >> Accepted()
      }
    case ar @ POST -> Root / "comments" as user =>
      ar.req.decodeR[CommentInput] { comment =>
        comments.create(user.id, comment) >> Created()
      }
    case GET -> Root / "comments" / UUIDVar(id) as _ =>
      comments.get(id.coerce[TaskId]).flatMap(Ok(_))
  }
}
