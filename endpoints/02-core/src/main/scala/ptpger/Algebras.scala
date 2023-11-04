package ptpger

import cats.MonadThrow
import cats.effect.std.Random
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.SCrypt
import uz.scala.aws.s3.S3Client

import ptpger.algebras.AssetsAlgebra
import ptpger.algebras.MessagesAlgebra
import ptpger.algebras.TaskAlgebra
import ptpger.algebras.UsersAlgebra
import ptpger.auth.impl.Auth
import ptpger.domain.AuthedUser
import ptpger.effects.Calendar
import ptpger.effects.GenUUID
import ptpger.integrations.opersms.OperSmsClient

case class Algebras[F[_]](
    auth: Auth[F, AuthedUser],
    tasks: TaskAlgebra[F],
    assets: AssetsAlgebra[F],
    users: UsersAlgebra[F],
    messages: MessagesAlgebra[F],
  )

object Algebras {
  def make[F[_]: MonadThrow: Calendar: GenUUID: Random: Lambda[M[_] => fs2.Compiler[M, M]]](
      auth: Auth[F, AuthedUser],
      repositories: Repositories[F],
      s3Client: S3Client[F],
      operSmsClient: OperSmsClient[F],
    )(implicit
      P: PasswordHasher[F, SCrypt]
    ): Algebras[F] = {
    val Repositories(users, tasks, comments, actions, assets, messages) = repositories
    val messagesAlgebra = MessagesAlgebra.make[F](messages, operSmsClient)
    Algebras[F](
      auth = auth,
      tasks = TaskAlgebra.make[F](tasks, comments, actions, users),
      assets = AssetsAlgebra.make[F](assets, s3Client),
      users = UsersAlgebra.make[F](repositories.users, messagesAlgebra),
      messages = messagesAlgebra,
    )
  }
}
