package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds weekly shower tracking without changing or deleting any existing data.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(
        database: SupportSQLiteDatabase
    ) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shower_logs` (
                `date` TEXT NOT NULL,
                `completedAt` INTEGER NOT NULL,
                PRIMARY KEY(`date`)
            )
            """.trimIndent()
        )
    }
}
