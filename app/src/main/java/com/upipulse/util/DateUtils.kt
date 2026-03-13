package com.upipulse.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object DateUtils {
    fun currentMonthRange(zone: ZoneId = ZoneId.systemDefault()): ClosedRange<Instant> {
        val today = LocalDate.now(zone)
        val start = today.withDayOfMonth(1).atStartOfDay(zone).toInstant()
        val end = today.withDayOfMonth(today.lengthOfMonth()).plusDays(1)
            .atStartOfDay(zone).minusNanos(1).toInstant()
        return start..end
    }

    fun currentWeekRange(zone: ZoneId = ZoneId.systemDefault()): ClosedRange<Instant> {
        val today = LocalDate.now(zone)
        val startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant()
        return start..end
    }

    fun weekDays(): List<DayOfWeek> = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    fun Instant.isWithin(range: ClosedRange<Instant>): Boolean =
        this >= range.start && this <= range.endInclusive
}
