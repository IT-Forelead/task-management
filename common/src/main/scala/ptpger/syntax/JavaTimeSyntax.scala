package ptpger.syntax

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime

trait JavaTimeSyntax {
  final implicit def localTimeOps(dateTime: LocalTime): LocalTimeOps =
    new LocalTimeOps(dateTime)

  implicit def localDateTimeOps(ldt: LocalDateTime): LocalDateTimeOps =
    new LocalDateTimeOps(ldt)

  final implicit def instantSyntax(dateTime: Instant): InstantOps =
    new InstantOps(dateTime)
}

final class LocalTimeOps(private val self: LocalTime) extends AnyVal {
  def noNanos: LocalTime =
    self.plusNanos(500).truncatedTo(java.time.temporal.ChronoUnit.MICROS)
}

final class LocalDateTimeOps(private val self: LocalDateTime) {
  def endOfDay: LocalDateTime = self.withHour(23).withMinute(59).withSecond(59)
  def noNanos: LocalDateTime =
    self.plusNanos(500).truncatedTo(java.time.temporal.ChronoUnit.MICROS)
}
final class InstantOps(private val self: Instant) extends AnyVal {
  def noNanos: Instant =
    self.plusNanos(500).truncatedTo(java.time.temporal.ChronoUnit.MICROS)
}
