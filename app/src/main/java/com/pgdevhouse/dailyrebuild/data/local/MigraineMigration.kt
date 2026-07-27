package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds occasional migraine / visual-aura event tracking without changing or
 * deleting any existing Daily Rebuild data.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(
        database: SupportSQLiteDatabase
    ) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `migraine_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` TEXT NOT NULL,
                `occurredAt` INTEGER NOT NULL,
                `auraDurationMinutes` INTEGER,
                `visualAura` INTEGER NOT NULL,
                `headPain` INTEGER NOT NULL,
                `foggyAfterward` INTEGER NOT NULL,
                `notes` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_migraine_logs_date` " +
                "ON `migraine_logs` (`date`)"
        )
    }
}
