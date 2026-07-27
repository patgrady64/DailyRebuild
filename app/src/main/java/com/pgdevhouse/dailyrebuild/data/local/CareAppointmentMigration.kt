package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds planned care appointments and reminder/planning information. */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `care_appointments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `placeId` INTEGER,
                `providerId` INTEGER,
                `date` TEXT NOT NULL,
                `scheduledAt` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `visitCategory` TEXT NOT NULL,
                `visitFormat` TEXT NOT NULL,
                `placeName` TEXT NOT NULL,
                `placeCategory` TEXT NOT NULL,
                `providerName` TEXT NOT NULL,
                `providerCredentials` TEXT NOT NULL,
                `providerSpecialty` TEXT NOT NULL,
                `address` TEXT NOT NULL,
                `city` TEXT NOT NULL,
                `state` TEXT NOT NULL,
                `zipCode` TEXT NOT NULL,
                `placePhone` TEXT NOT NULL,
                `providerPhone` TEXT NOT NULL,
                `reasonForAppointment` TEXT NOT NULL,
                `transportationMode` TEXT NOT NULL,
                `transportationDetails` TEXT NOT NULL,
                `leaveByAt` INTEGER,
                `transportationConfirmed` INTEGER NOT NULL,
                `questionsToAsk` TEXT NOT NULL,
                `documentsToBring` TEXT NOT NULL,
                `preparationNotes` TEXT NOT NULL,
                `remindOneDayBefore` INTEGER NOT NULL,
                `remindTwoHoursBefore` INTEGER NOT NULL,
                `convertedVisitId` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_appointments_date` " +
                "ON `care_appointments` (`date`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_appointments_scheduledAt` " +
                "ON `care_appointments` (`scheduledAt`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_appointments_status` " +
                "ON `care_appointments` (`status`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_appointments_placeId` " +
                "ON `care_appointments` (`placeId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_appointments_providerId` " +
                "ON `care_appointments` (`providerId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_appointments_convertedVisitId` " +
                "ON `care_appointments` (`convertedVisitId`)"
        )
    }
}
