package uz.scala.http4s.utils

import cats.data.ValidatedNec

object MapConvert {
  type ValidationResult[A] = ValidatedNec[String, A]
}

trait MapConvert[F[_], A] {
  def fromMap(values: Map[String, String]): F[A]
}
