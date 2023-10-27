package ptpger.repos

import cats.effect.Async
import cats.effect.Resource
import ptpger.Phone
import ptpger.domain.AuthedUser.User
import ptpger.domain.auth.AccessCredentials
import ptpger.repos.sql.UsersSql
import skunk._
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps
trait UsersRepository[F[_]] {
  def find(phone: Phone): F[Option[AccessCredentials[User]]]
  def create(userAndHash: AccessCredentials[User]): F[Unit]
}

object UsersRepository {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): UsersRepository[F] = new UsersRepository[F] {
    override def find(phone: Phone): F[Option[AccessCredentials[User]]] =
      UsersSql.findByLogin.queryOption(phone)

    override def create(userAndHash: AccessCredentials[User]): F[Unit] =
      UsersSql.insert.execute(userAndHash)
  }
}
