package ptpger

import cats.MonadThrow

import ptpger.algebras.TaskAlgebra
import ptpger.auth.impl.Auth
import ptpger.domain.AuthedUser
import ptpger.effects.Calendar
import ptpger.effects.GenUUID

case class Algebras[F[_]](
    auth: Auth[F, AuthedUser],
    tasks: TaskAlgebra[F],
  )

object Algebras {
  def make[F[_]: MonadThrow: Calendar: GenUUID](
      auth: Auth[F, AuthedUser],
      repositories: Repositories[F],
    ): Algebras[F] = {
    val Repositories(users, tasks, comments, actions) = repositories
    Algebras[F](
      auth = auth,
      tasks = TaskAlgebra.make[F](tasks, comments, actions, users),
    )
  }
}
