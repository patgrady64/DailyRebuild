package com.pgdevhouse.dailyrebuild.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTableCatalogTest {
    @Test
    fun backupCatalogContainsEveryVersion14UserTableExactlyOnce() {
        val expected = setOf(
            "daily_records",
            "food_products",
            "food_log_entries",
            "saved_meals",
            "saved_meal_ingredients",
            "daily_activity_snapshots",
            "mobility_sessions",
            "health_profile",
            "health_measurements",
            "pain_activity_logs",
            "medication_entries",
            "calorie_goal_changes",
            "shower_logs",
            "migraine_logs",
            "saved_meetings",
            "meeting_attendance",
            "care_places",
            "care_providers",
            "care_visits",
            "care_appointments",
            "pantry_essentials"
        )

        val actual = DailyRebuildBackupManager.INSERT_ORDER

        assertEquals(expected, actual.toSet())
        assertEquals(expected.size, actual.size)
        assertEquals(actual.asReversed(), DailyRebuildBackupManager.DELETE_ORDER)
        assertTrue(
            actual.indexOf("food_products") <
                actual.indexOf("food_log_entries")
        )
        assertTrue(
            actual.indexOf("saved_meals") <
                actual.indexOf("saved_meal_ingredients")
        )
    }
}
