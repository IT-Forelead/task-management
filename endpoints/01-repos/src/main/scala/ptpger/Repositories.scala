package ptpger

import cats.effect.Async
import cats.effect.Resource
import skunk.Session

import ptpger.repos.TaskCommentsRepository
import ptpger.repos.TasksRepository
import ptpger.repos.UsersRepository

case class Repositories[F[_]](
    users: UsersRepository[F],
    tasks: TasksRepository[F],
    comments: TaskCommentsRepository[F],
  )
object Repositories {
  def make[F[_]: Async](
      implicit
      session: Resource[F, Session[F]]
    ): Repositories[F] =
    Repositories(
      users = UsersRepository.make[F],
      tasks = TasksRepository.make[F],
      comments = TaskCommentsRepository.make[F],
    )
}
