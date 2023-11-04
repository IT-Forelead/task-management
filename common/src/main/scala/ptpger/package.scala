import cats.Monad
import cats.effect.std.Random
import cats.implicits.toFunctorOps
import cats.implicits.toTraverseOps
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

package object ptpger {
  type Phone = String Refined MatchesRegex[W.`"""[+][\\d]+"""`.T]

  def randomStr[F[_]: Random: Monad](n: Int): F[String] =
    (0 to n)
      .toList
      .traverse { _ =>
        Random[F].nextAlphaNumeric
      }
      .map(_.filter(_.isLetter).mkString)
}
