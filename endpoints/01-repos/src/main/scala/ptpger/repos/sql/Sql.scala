package ptpger.repos.sql

import skunk.Codec

import ptpger.effects.IsUUID

abstract class Sql[T: IsUUID] {
  val id: Codec[T] = identification[T]
}
