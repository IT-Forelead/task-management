package ptpger.syntax

import cats.Monad
import cats.data.OptionT
import cats.syntax.all._

trait OptionSyntax {
  implicit def optionSyntaxFunctorBooleanOps[F[_]: Monad](fa: F[Boolean]): FunctorBooleanOps[F] =
    new FunctorBooleanOps(fa)
  implicit def optionSyntaxBooleanOps(cond: Boolean): BooleanOps =
    new BooleanOps(cond)
}
final class FunctorBooleanOps[F[_]: Monad](fa: F[Boolean]) {
  def asOptionT: OptionT[F, Unit] =
    OptionT {
      fa.ifM(().some.pure[F], none[Unit].pure[F])
    }
}

final class BooleanOps(cond: Boolean) {
  def asOption: Option[Unit] =
    if (cond) ().some else none[Unit]
}
