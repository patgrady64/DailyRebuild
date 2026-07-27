package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(
            database: SupportSQLiteDatabase
        ) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS daily_activity_snapshots (
                    date TEXT NOT NULL,
                    steps INTEGER NOT NULL DEFAULT 0,
                    distanceMiles REAL NOT NULL DEFAULT 0,
                    activeCalories REAL NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(date)
                )
                """.trimIndent()
            )
        }
    }
