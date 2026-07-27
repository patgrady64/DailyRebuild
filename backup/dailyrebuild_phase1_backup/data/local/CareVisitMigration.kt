package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds reusable care places/providers and completed visit history. */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `care_places` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `placeCategory` TEXT NOT NULL,
                `address` TEXT NOT NULL,
                `city` TEXT NOT NULL,
                `state` TEXT NOT NULL,
                `zipCode` TEXT NOT NULL,
                `phone` TEXT NOT NULL,
                `website` TEXT NOT NULL,
                `patientPortal` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `active` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_places_name_city_state` " +
                "ON `care_places` (`name`, `city`, `state`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_places_active` " +
                "ON `care_places` (`active`)"
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `care_providers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `placeId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `credentials` TEXT NOT NULL,
                `specialty` TEXT NOT NULL,
                `phone` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `active` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_providers_placeId` " +
                "ON `care_providers` (`placeId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_providers_name` " +
                "ON `care_providers` (`name`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_providers_active` " +
                "ON `care_providers` (`active`)"
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `care_visits` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `placeId` INTEGER,
                `providerId` INTEGER,
                `date` TEXT NOT NULL,
                `startedAt` INTEGER NOT NULL,
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
                `reasonForVisit` TEXT NOT NULL,
                `visitSummary` TEXT NOT NULL,
                `testsProcedures` TEXT NOT NULL,
                `resultsDiscussed` TEXT NOT NULL,
                `instructions` TEXT NOT NULL,
                `medicationChanges` TEXT NOT NULL,
                `referrals` TEXT NOT NULL,
                `followUpDate` TEXT,
                `notes` TEXT NOT NULL,
                `weightPounds` REAL,
                `systolic` INTEGER,
                `diastolic` INTEGER,
                `a1c` REAL,
                `bloodGlucose` REAL,
                `cholesterolTotal` REAL,
                `cholesterolLdl` REAL,
                `cholesterolHdl` REAL,
                `triglycerides` REAL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_visits_date` " +
                "ON `care_visits` (`date`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_visits_startedAt` " +
                "ON `care_visits` (`startedAt`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_visits_placeId` " +
                "ON `care_visits` (`placeId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_visits_providerId` " +
                "ON `care_visits` (`providerId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_visits_visitCategory` " +
                "ON `care_visits` (`visitCategory`)"
        )
    }
}
