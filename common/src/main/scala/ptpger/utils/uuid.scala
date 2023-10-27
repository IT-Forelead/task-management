package ptpger.utils

import scala.annotation.implicitNotFound

import derevo.Derivation
import derevo.NewTypeDerivation

import ptpger.effects.IsUUID

object uuid extends Derivation[IsUUID] with NewTypeDerivation[IsUUID] {
  def instance(implicit ev: OnlyNewtypes): Nothing = ev.absurd

  @implicitNotFound("Only newtypes instances can be derived")
  final abstract class OnlyNewtypes {
    def absurd: Nothing = ???
  }
}
