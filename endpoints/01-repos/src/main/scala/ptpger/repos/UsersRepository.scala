package ptpger.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.toFunctorOps
import skunk._
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps

import ptpger.Phone
import ptpger.domain.AuthedUser.User
import ptpger.domain.PersonId
import ptpger.domain.args.users.UserFilters
import ptpger.domain.auth.AccessCredentials
import ptpger.repos.sql.UsersSql
trait UsersRepository[F[_]] {
  def find(phone: Phone): F[Option[AccessCredentials[User]]]
  def create(userAndHash: AccessCredentials[User]): F[Unit]
  def findByIds(ids: NonEmptyList[PersonId]): F[Map[PersonId, User]]
  def get(filters: UserFilters): F[List[User]]
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

    override def findByIds(ids: NonEmptyList[PersonId]): F[Map[PersonId, User]] = {
      val personIds = ids.toList
      UsersSql.findByIds(personIds).queryList(personIds).map(_.map(a => a.id -> a).toMap)
    }
    override def get(filters: UserFilters): F[List[User]] = {
      val af = UsersSql.select(filters)
      af.fragment.query(UsersSql.codec).queryList(af.argument)
    }
  }
}
