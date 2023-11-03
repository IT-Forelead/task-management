package ptpger

import cats.MonadThrow
import uz.scala.aws.s3.S3Client

import ptpger.algebras.AssetsAlgebra
import ptpger.algebras.TaskAlgebra
import ptpger.auth.impl.Auth
import ptpger.domain.AuthedUser
import ptpger.effects.Calendar
import ptpger.effects.GenUUID

case class Algebras[F[_]](
    auth: Auth[F, AuthedUser],
    tasks: TaskAlgebra[F],
    assets: AssetsAlgebra[F],
  )

object Algebras {
  def make[F[_]: MonadThrow: Calendar: GenUUID: Lambda[M[_] => fs2.Compiler[M, M]]](
      auth: Auth[F, AuthedUser],
      repositories: Repositories[F],
      s3Client: S3Client[F],
    ): Algebras[F] = {
    val Repositories(users, tasks, comments, actions, assets) = repositories
    Algebras[F](
      auth = auth,
      tasks = TaskAlgebra.make[F](tasks, comments, actions, users),
      assets = AssetsAlgebra.make[F](assets, s3Client),
    )
  }
}
