package ptpger.effects

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Duration
import java.time.ZonedDateTime

import cats.effect._
import uz.scala.syntax.javaTime._

trait Calendar[F[_]] {
  def currentDate: F[LocalDate]
  def currentDateTime: F[LocalDateTime]
  def currentZonedDateTime: F[ZonedDateTime]
  def currentInstant: F[Instant]
  def remainingDays(dueDate: LocalDate): F[Long]
}

object Calendar {
  def apply[F[_]](implicit C: Calendar[F]): Calendar[F] = C

  implicit def calendarForSync[F[_]](implicit F: Sync[F]): Calendar[F] =
    new Calendar[F] {
      override def currentDate: F[LocalDate] =
        F.delay(LocalDate.now)

      override def currentDateTime: F[LocalDateTime] =
        F.delay(LocalDateTime.now.noNanos)

      override def currentZonedDateTime: F[ZonedDateTime] =
        F.delay(ZonedDateTime.now.noNanos)

      override def currentInstant: F[Instant] =
        F.delay(Instant.now.noNanos)

      override def remainingDays(dueDate: LocalDate): F[Long] =
        F.map(currentDate) { current =>
            Duration.between(current.atStartOfDay(), dueDate.atStartOfDay()).toDays
        }
    }
}
