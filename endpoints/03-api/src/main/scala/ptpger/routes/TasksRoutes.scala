package ptpger.routes

import cats.MonadThrow
import cats.implicits.catsSyntaxFlatMapOps
import cats.implicits.toFlatMapOps
import io.estatico.newtype.ops._
import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import uz.scala.http4s.syntax.all.deriveEntityEncoder
import uz.scala.http4s.syntax.all.http4SyntaxReqOps
import uz.scala.http4s.utils.Routes

import ptpger.algebras.TaskAlgebra
import ptpger.domain._
import ptpger.domain.args.tasks._
final case class TasksRoutes[F[_]: JsonDecoder: MonadThrow](
    tasks: TaskAlgebra[F]
  ) extends Routes[F, AuthedUser] {
  override val path = "/tasks"

  override val public: HttpRoutes[F] = HttpRoutes.empty[F]

  override val `private`: AuthedRoutes[AuthedUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user =>
      ar.req.decodeR[TaskInput] { taskInput =>
        tasks.create(taskInput, user.fullName).flatMap(Created(_))
      }
    case ar @ POST -> Root as _ =>
      ar.req.decodeR[TaskFilters] { filters =>
        tasks.get(filters).flatMap(Ok(_))
      }
    case GET -> Root / "counts" as _ =>
      tasks.getCounts.flatMap(Ok(_))
    case GET -> Root / "counts" / UUIDVar(id) as _ =>
      tasks.getCountsByUserId(PersonId(id)).flatMap(Ok(_))
    case GET -> Root / "counts" / "all" as _ =>
      tasks.getCountsAll.flatMap(Ok(_))
    case ar @ PUT -> Root / UUIDVar(id) as user =>
      ar.req.decodeR[TaskAssignInput] { taskAssignInput =>
        tasks.assign(
          id.coerce[TaskId],
          taskAssignInput.userIds,
          user.fullName,
        ) >> Accepted()
      }
    case ar @ PUT -> Root / "edit" / UUIDVar(id) as user =>
      ar.req.decodeR[TaskUpdateInput] { taskInput =>
        tasks.update(id.coerce[TaskId], user.id, taskInput) >> Accepted()
      }
    case ar @ POST -> Root / "comments" as user =>
      ar.req.decodeR[CommentInput] { comment =>
        tasks.addComment(user.id, comment) >> Created()
      }
    case GET -> Root / "comments" / UUIDVar(id) as _ =>
      tasks.getComments(id.coerce[TaskId]).flatMap(Ok(_))
    case GET -> Root / "action-histories" / UUIDVar(id) as _ =>
      tasks.getActionHistories(id.coerce[TaskId]).flatMap(Ok(_))
  }
}
