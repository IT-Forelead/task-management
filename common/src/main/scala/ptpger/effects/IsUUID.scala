package ptpger.effects

import java.util.UUID

import monocle.Iso

trait IsUUID[A] {
  def uuid: Iso[UUID, A]
}

object IsUUID {
  def apply[A: IsUUID]: IsUUID[A] = implicitly

  implicit val identityUUID: IsUUID[UUID] = new IsUUID[UUID] {
    val uuid: Iso[UUID, UUID] = Iso[UUID, UUID](identity)(identity)
  }
}
