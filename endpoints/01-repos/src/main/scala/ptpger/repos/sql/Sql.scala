package ptpger.repos.sql

import ptpger.effects.IsUUID
import skunk.Codec

abstract class Sql[T: IsUUID] {
  val id: Codec[T] = identification[T]
}
