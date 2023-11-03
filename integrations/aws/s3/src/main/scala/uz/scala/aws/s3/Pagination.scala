package uz.scala.aws.s3

import cats.Applicative
import cats.implicits.toFunctorOps
import fs2._

object Pagination {
  sealed private trait PageIndicator[S]
  private case class FirstPage[S]() extends PageIndicator[S]
  private case class NextPage[S](token: S) extends PageIndicator[S]
  private case class NoMorePages[S]() extends PageIndicator[S]

  def offsetUnfoldChunkEval[F[_], S, O](
      f: Option[S] => F[(Chunk[O], Option[S])]
    )(implicit
      F: Applicative[F]
    ): Stream[F, O] = {
    def fetchPage(maybeNextPageToken: Option[S]): F[Option[(Chunk[O], PageIndicator[S])]] =
      f(maybeNextPageToken).map {
        case (segment, Some(nextToken)) => Option((segment, NextPage(nextToken)))
        case (segment, None) => Option((segment, NoMorePages[S]()))
      }

    Stream.unfoldChunkEval[F, PageIndicator[S], O](FirstPage[S]()) {
      case FirstPage() => fetchPage(None)
      case NextPage(token) => fetchPage(Some(token))
      case NoMorePages() => F.pure(None)
    }
  }
}
