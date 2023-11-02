package ptpger.repos

import cats.effect.Async
import cats.effect.Resource
import skunk._
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps

import ptpger.Phone
import ptpger.domain.AuthedUser.User
import ptpger.domain.PersonId
import ptpger.domain.auth.AccessCredentials
import ptpger.repos.sql.UsersSql
trait UsersRepository[F[_]] {
  def find(phone: Phone): F[Option[AccessCredentials[User]]]
  def create(userAndHash: AccessCredentials[User]): F[Unit]
  def findById(id: PersonId): F[Option[User]]
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

    override def findById(id: PersonId): F[Option[User]] =
      UsersSql.findById.queryOption(id)
  }
}
