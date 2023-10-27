package uz.scala.http4s.utils

import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

abstract class Routes[F[_], U] extends Http4sDsl[F] {
  val path: String
  val public: HttpRoutes[F]
  val `private`: AuthedRoutes[U, F]
}
