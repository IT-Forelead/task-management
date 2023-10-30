package uz.scala

import _root_.sttp.client3.ResponseException
import io.circe.Error
import io.circe.Json

package object sttp {
  type CirceJsonResponse = CirceJsonResponseM[cats.Id]
  type CirceJsonResponseM[M[_]] = Either[ResponseException[String, Error], M[Json]]
}
