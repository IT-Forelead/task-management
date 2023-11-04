package ptpger.algebras

import ptpger.domain.AuthedUser.User
import ptpger.domain.args.users.UserFilters
import ptpger.repos.UsersRepository

trait UsersAlgebra[F[_]] {
  def get(filters: UserFilters): F[List[User]]
}
object UsersAlgebra {
  def make[F[_]](usersRepository: UsersRepository[F]): UsersAlgebra[F] =
    new UsersAlgebra[F] {
      override def get(filters: UserFilters): F[List[User]] =
        usersRepository.get(filters)
    }
}