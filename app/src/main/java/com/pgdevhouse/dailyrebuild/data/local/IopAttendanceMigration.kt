package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds explicit missed-IOP exceptions while keeping attendance automatic. */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `iop_missed_occurrences` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `groupId` INTEGER NOT NULL,
                `occurrenceDate` TEXT NOT NULL,
                `groupNameSnapshot` TEXT NOT NULL,
                `startMinutesSnapshot` INTEGER NOT NULL,
                `endMinutesSnapshot` INTEGER NOT NULL,
                `reason` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_iop_missed_occurrences_groupId_occurrenceDate` " +
                "ON `iop_missed_occurrences` (`groupId`, `occurrenceDate`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS " +
                "`index_iop_missed_occurrences_occurrenceDate` " +
                "ON `iop_missed_occurrences` (`occurrenceDate`)"
        )
    }
}
