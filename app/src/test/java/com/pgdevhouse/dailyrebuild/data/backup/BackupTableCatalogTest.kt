package com.pgdevhouse.dailyrebuild.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTableCatalogTest {
    @Test
    fun backupCatalogContainsEveryVersion21UserTableExactlyOnce() {
        val expected = setOf(
            "daily_records",
            "food_products",
            "food_log_entries",
            "drink_definitions",
            "drink_entries",
            "prepared_food_leftovers",
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
            "pantry_essentials",
            "life_maintenance_logs",
            "iop_groups",
            "iop_missed_occurrences"
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
        assertTrue(
            actual.indexOf("drink_definitions") <
                actual.indexOf("drink_entries")
        )
        assertTrue(
            actual.indexOf("food_log_entries") <
                actual.indexOf("prepared_food_leftovers")
        )
        assertEquals(21, DailyRebuildBackupManager.DATABASE_VERSION)
        assertEquals(2, DailyRebuildBackupManager.FORMAT_VERSION)
    }
}
