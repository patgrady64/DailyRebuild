package com.pgdevhouse.dailyrebuild.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyPainPolicyTest {
    @Test
    fun lowerLaterEntryDoesNotReduceDailyHigh() {
        assertEquals(4f, recordDailyHighestPain(4f, 2f), 0f)
    }

    @Test
    fun higherLaterEntryRaisesDailyHigh() {
        assertEquals(7f, recordDailyHighestPain(4f, 7f), 0f)
    }

    @Test
    fun backAndShinHighsCanBeUpdatedIndependently() {
        val backPain = recordDailyHighestPain(4f, 7f)
        val shinPain = recordDailyHighestPain(6f, 3f)

        assertEquals(7f, backPain, 0f)
        assertEquals(6f, shinPain, 0f)
    }

    @Test
    fun valuesAreClampedToPainScale() {
        assertEquals(10f, recordDailyHighestPain(12f, 20f), 0f)
    }
}
