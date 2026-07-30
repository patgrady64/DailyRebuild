package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds occasional life-maintenance completion history without changing existing data. */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `life_maintenance_logs` (
                `taskKey` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `completedAt` INTEGER NOT NULL,
                PRIMARY KEY(`taskKey`, `date`)
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_life_maintenance_logs_date`
            ON `life_maintenance_logs` (`date`)
            """.trimIndent()
        )
    }
}
