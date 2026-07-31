package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds recurring IOP group schedules and seeds the current Monday–Thursday schedule. */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `iop_groups` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `dayOfWeek` INTEGER NOT NULL,
                `startMinutes` INTEGER NOT NULL,
                `endMinutes` INTEGER NOT NULL,
                `location` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `active` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_iop_groups_dayOfWeek_startMinutes` " +
                "ON `iop_groups` (`dayOfWeek`, `startMinutes`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_iop_groups_active` " +
                "ON `iop_groups` (`active`)"
        )

        val now = System.currentTimeMillis()
        (1..4).forEach { day ->
            database.execSQL(
                """
                INSERT INTO `iop_groups`
                (`name`, `dayOfWeek`, `startMinutes`, `endMinutes`, `location`, `notes`, `active`, `createdAt`, `updatedAt`)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    "IOP Group",
                    day,
                    18 * 60 + 30,
                    20 * 60 + 30,
                    "",
                    "",
                    1,
                    now,
                    now
                )
            )
        }
    }
}
