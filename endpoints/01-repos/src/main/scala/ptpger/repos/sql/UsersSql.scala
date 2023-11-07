package ptpger.repos.sql

import shapeless.HNil
import skunk._
import skunk.implicits._
import uz.scala.skunk.syntax.all.skunkSyntaxFragmentOps

import ptpger.Phone
import ptpger.domain.AuthedUser.User
import ptpger.domain.PersonId
import ptpger.domain.args.users.UserFilters
import ptpger.domain.auth.AccessCredentials
import ptpger.domain.enums.UserEmployment

private[repos] object UsersSql extends Sql[PersonId] {
  private[repos] val codec = (id *: zonedDateTime *: nes *: nes *: role *: phone).to[User]
  private val accessCredentialsDecoder: Decoder[AccessCredentials[User]] =
    (codec *: passwordHash).map {
      case user *: hash *: HNil =>
        AccessCredentials(
          data = user,
          password = hash,
        )
    }

  val findByLogin: Query[Phone, AccessCredentials[User]] =
    sql"""SELECT id, created_at, firstname, lastname, role, phone, password FROM users
          WHERE phone = $phone LIMIT 1""".query(accessCredentialsDecoder)

  def findByIds(ids: List[PersonId]): Query[ids.type, User] =
    sql"""SELECT id, created_at, firstname, lastname, role, phone FROM users
          WHERE id IN ${id.values.list(ids)}""".query(codec)

  val insert: Command[AccessCredentials[User]] =
    sql"""INSERT INTO users VALUES ($id, $zonedDateTime, $nes, $nes, $phone, $role, $passwordHash)"""
      .command
      .contramap { (u: AccessCredentials[User]) =>
        u.data.id *: u.data.createdAt *: u.data.firstname *: u.data.lastname *: u.data.phone *:
          u.data.role *: u.password *: EmptyTuple
      }

  private def searchFilter(filters: UserFilters): List[Option[AppliedFragment]] =
    List(
      filters.id.map(sql"u.id = $id"),
      filters.employment.map {
        case UserEmployment.Employed =>
          sql"t.user_id IS NOT NULL".apply(Void)
        case UserEmployment.Unemployed =>
          sql"t.user_id IS NULL".apply(Void)
      },
    )

  def select(filters: UserFilters): AppliedFragment = {
    val baseQuery: Fragment[Void] =
      sql"""SELECT DISTINCT ON(u.id) u.id, u.created_at, u.firstname, u.lastname, u.role, phone FROM users u
           LEFT JOIN tasks t on t.user_id = u.id or t.user_id IS NULL
         """
    baseQuery(Void).whereAndOpt(searchFilter(filters))
  }
}
