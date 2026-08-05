package com.pgdevhouse.dailyrebuild

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeeklyReportPeriodTest {
    @Test
    fun previousCompletedWeek_usesMondayThroughSunday() {
        val period = WeeklyReportPeriod.previousCompletedWeek(
            LocalDate.of(2026, 8, 5)
        )

        assertEquals(LocalDate.of(2026, 7, 27), period.start)
        assertEquals(LocalDate.of(2026, 8, 2), period.end)
    }

    @Test
    fun previousCompletedWeek_onMonday_doesNotUseCurrentDay() {
        val period = WeeklyReportPeriod.previousCompletedWeek(
            LocalDate.of(2026, 8, 3)
        )

        assertEquals(LocalDate.of(2026, 7, 27), period.start)
        assertEquals(LocalDate.of(2026, 8, 2), period.end)
    }
}
