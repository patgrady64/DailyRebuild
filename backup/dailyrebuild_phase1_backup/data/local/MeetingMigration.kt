package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds reusable meetings and attendance history without touching older data. */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(
        database: SupportSQLiteDatabase
    ) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `saved_meetings` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `address` TEXT NOT NULL,
                `city` TEXT NOT NULL,
                `state` TEXT NOT NULL,
                `zipCode` TEXT NOT NULL,
                `usualDayOfWeek` INTEGER,
                `usualStartMinutes` INTEGER,
                `typicalDurationMinutes` INTEGER NOT NULL,
                `favorite` INTEGER NOT NULL,
                `notes` TEXT NOT NULL,
                `active` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_saved_meetings_name_city_state` " +
                "ON `saved_meetings` (`name`, `city`, `state`)"
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_saved_meetings_active` " +
                "ON `saved_meetings` (`active`)"
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `meeting_attendance` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `savedMeetingId` INTEGER,
                `date` TEXT NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `durationMinutes` INTEGER NOT NULL,
                `meetingName` TEXT NOT NULL,
                `address` TEXT NOT NULL,
                `city` TEXT NOT NULL,
                `state` TEXT NOT NULL,
                `zipCode` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_meeting_attendance_date` " +
                "ON `meeting_attendance` (`date`)"
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_meeting_attendance_savedMeetingId` " +
                "ON `meeting_attendance` (`savedMeetingId`)"
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_meeting_attendance_startedAt` " +
                "ON `meeting_attendance` (`startedAt`)"
        )
    }
}
