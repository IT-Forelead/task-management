package ptpger.algebras

import cats.MonadThrow
import cats.effect.std.Random
import cats.implicits.toFlatMapOps
import cats.implicits.toFunctorOps
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.SCrypt
import uz.scala.syntax.refined.commonSyntaxAutoRefineV

import ptpger.domain.AuthedUser.User
import ptpger.domain.PersonId
import ptpger.domain.args.users.UserFilters
import ptpger.domain.args.users.UserInput
import ptpger.domain.auth.AccessCredentials
import ptpger.domain.enums.Role
import ptpger.effects.Calendar
import ptpger.effects.GenUUID
import ptpger.randomStr
import ptpger.repos.UsersRepository
import ptpger.utils.ID

trait UsersAlgebra[F[_]] {
  def get(filters: UserFilters): F[List[User]]
  def create(userInput: UserInput): F[PersonId]
}
object UsersAlgebra {
  def make[F[_]: MonadThrow: Calendar: GenUUID: Random](
      usersRepository: UsersRepository[F],
      messages: MessagesAlgebra[F],
    )(implicit
      P: PasswordHasher[F, SCrypt]
    ): UsersAlgebra[F] =
    new UsersAlgebra[F] {
      override def get(filters: UserFilters): F[List[User]] =
        usersRepository.get(filters)

      override def create(userInput: UserInput): F[PersonId] =
        for {
          id <- ID.make[F, PersonId]
          now <- Calendar[F].currentZonedDateTime
          user = User(
            id = id,
            createdAt = now,
            firstname = userInput.firstname,
            lastname = userInput.lastname,
            role = Role.Creator,
            phone = userInput.phone,
          )
          password <- randomStr[F](8)

          hash <- SCrypt.hashpw[F](password)

          accessCredentials = AccessCredentials(user, hash)
          _ <- usersRepository.create(accessCredentials)
          smsText =
            s"\nСиз платформага муваффақиятли рўйхатдан ўтдингиз\nлогин: ${user.phone}\nпарол: $password"
          _ <- messages.sendSms(user.phone, smsText)
        } yield id
    }
}
