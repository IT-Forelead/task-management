package ptpger.syntax

import scala.collection.Factory

import cats.syntax.all._
import eu.timepit.refined.api.RefType
import eu.timepit.refined.api.Refined
import eu.timepit.refined.api.Validate
import eu.timepit.refined.refineV
import shapeless.<:!<
import shapeless.tag.@@

trait RefinedSyntax {

  /** Unsafely tries to coerce a runtime value to a refined type.
    *
    * @tparam T
    *   the original type
    * @tparam P
    *   the refined predicate
    * @param unrefined
    *   the runtime value
    */
  implicit def commonSyntaxAutoRefineV[T, P](
      unrefined: T
    )(implicit
      validate: Validate[T, P],
      refType: RefType[Refined],
    ): Refined[T, P] =
    refType.refine[P](unrefined).valueOr(err => throw new IllegalArgumentException(err))

  implicit def commonSyntaxAutoRefineT[T, P](
      unrefined: T
    )(implicit
      validate: Validate[T, P],
      refType: RefType[@@],
    ): @@[T, P] =
    refType.refine[P](unrefined).valueOr(err => throw new IllegalArgumentException(err))

  /** Unwraps a refined value into its unrefined counterpart. */
  implicit def commonSyntaxAutoUnwrapV[T, P](refined: Refined[T, P]): T = refined.value

  /** Unsafely tries to coerce an optional runtime value to a refined type.
    *
    * @tparam T
    *   the original type
    * @tparam P
    *   the refined predicate
    * @param unrefinedOpt
    *   the optional runtime value
    */
  implicit def commonSyntaxAutoRefineOptV[T, P](
      unrefinedOpt: Option[T]
    )(implicit
      validate: Validate[T, P]
    ): Option[Refined[T, P]] =
    unrefinedOpt.flatMap(v => refineV[P](v).fold(_ => Option.empty[Refined[T, P]], v => Some(v)))

  implicit def commonSyntaxAutoRefineTraversable[T, P, C[X] <: Iterable[X]](
      unrefined: C[T]
    )(implicit
      validate: Validate[T, P],
      ev: T <:!< Refined[_, _],
      factory: Factory[Refined[T, P], C[Refined[T, P]]],
    ): C[Refined[T, P]] =
    unrefined
      .map(v => refineV[P](v).valueOr(err => throw new IllegalArgumentException(err)))
      .to(factory)
}
