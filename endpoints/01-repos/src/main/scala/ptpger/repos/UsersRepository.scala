package ptpger.repos

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.implicits.toFunctorOps
import skunk._
import skunk.codec.all.int8
import uz.scala.skunk.syntax.all.skunkSyntaxCommandOps
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps
import uz.scala.skunk.syntax.all.skunkSyntaxQueryOps
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import ptpger.Phone
import ptpger.domain.AuthedUser.User
import ptpger.domain.PersonId
import ptpger.domain.ResponseData
import ptpger.domain.args.users.UserFilters
import ptpger.domain.auth.AccessCredentials
import ptpger.repos.sql.UsersSql
trait UsersRepository[F[_]] {
  def find(phone: Phone): F[Option[AccessCredentials[User]]]
  def create(userAndHash: AccessCredentials[User]): F[Unit]
  def findByIds(ids: NonEmptyList[PersonId]): F[Map[PersonId, User]]
  def get(filters: UserFilters): F[ResponseData[User]]
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
    override def get(filters: UserFilters): F[ResponseData[User]] = {
      val af = UsersSql
        .select(filters)
        .paginateOpt(filters.limit.map(_.value), filters.offset.map(_.value))
      af.fragment.query(UsersSql.codec *: int8).queryList(af.argument).map { users =>
        ResponseData(users.map(_.head), users.headOption.fold(0L)(_.tail.head))
      }
    }
  }
}
