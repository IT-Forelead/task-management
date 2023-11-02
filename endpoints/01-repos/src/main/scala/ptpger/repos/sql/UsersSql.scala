package ptpger.repos.sql

import shapeless.HNil
import skunk._
import skunk.implicits._

import ptpger.Phone
import ptpger.domain.AuthedUser.User
import ptpger.domain.PersonId
import ptpger.domain.auth.AccessCredentials

private[repos] object UsersSql extends Sql[PersonId] {
  private val codec = (id *: zonedDateTime *: nes *: nes *: role *: phone).to[User]
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

  val findById: Query[PersonId, User] =
    sql"""SELECT id, created_at, firstname, lastname, role, phone FROM users
          WHERE id = $id LIMIT 1""".query(codec)

  val insert: Command[AccessCredentials[User]] =
    sql"""INSERT INTO users VALUES ($id, $zonedDateTime, $nes, $nes, $role, $phone, $passwordHash)"""
      .command
      .contramap { (u: AccessCredentials[User]) =>
        u.data.id *: u.data.createdAt *: u.data.firstname *: u.data.lastname *: u.data.role *:
          u.data.phone *: u.password *: EmptyTuple
      }
}
