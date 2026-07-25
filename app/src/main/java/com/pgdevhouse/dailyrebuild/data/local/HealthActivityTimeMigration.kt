package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Replaces the unused active-calorie snapshot value with recorded activity
 * time. Existing snapshots keep their steps and distance; their activity time
 * starts at zero until the day is saved again with Health Connect data.
 */
val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(
            database: SupportSQLiteDatabase
        ) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS daily_activity_snapshots_new (
                    date TEXT NOT NULL,
                    steps INTEGER NOT NULL DEFAULT 0,
                    distanceMiles REAL NOT NULL DEFAULT 0,
                    activityMinutes INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(date)
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO daily_activity_snapshots_new (
                    date,
                    steps,
                    distanceMiles,
                    activityMinutes,
                    updatedAt
                )
                SELECT
                    date,
                    steps,
                    distanceMiles,
                    0,
                    updatedAt
                FROM daily_activity_snapshots
                """.trimIndent()
            )

            database.execSQL(
                "DROP TABLE daily_activity_snapshots"
            )

            database.execSQL(
                """
                ALTER TABLE daily_activity_snapshots_new
                RENAME TO daily_activity_snapshots
                """.trimIndent()
            )
        }
    }
