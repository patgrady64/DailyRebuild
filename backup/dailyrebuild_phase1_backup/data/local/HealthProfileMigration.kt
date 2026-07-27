package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS health_profile (
                id INTEGER NOT NULL PRIMARY KEY,
                birthDate TEXT NOT NULL,
                heightInches INTEGER NOT NULL,
                approximateStartingWeightPounds REAL,
                weightGoalPounds REAL,
                conditionsSummary TEXT NOT NULL,
                foodConstraints TEXT NOT NULL,
                movementLimitations TEXT NOT NULL,
                mobilitySeatedDefault INTEGER NOT NULL,
                mobilityBedDefault INTEGER NOT NULL,
                mobilityFloorDefault INTEGER NOT NULL,
                mobilityStandingDefault INTEGER NOT NULL,
                currentCalorieGoal INTEGER,
                medicationImportCompleted INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS health_measurements (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recordedDate TEXT NOT NULL,
                type TEXT NOT NULL,
                primaryValue REAL NOT NULL,
                secondaryValue REAL,
                tertiaryValue REAL,
                quaternaryValue REAL,
                notes TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_health_measurements_recordedDate_type
            ON health_measurements(recordedDate, type)
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pain_activity_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recordedDate TEXT NOT NULL,
                activityType TEXT NOT NULL,
                bodyArea TEXT NOT NULL,
                painBefore INTEGER NOT NULL,
                painAfter INTEGER NOT NULL,
                durationMinutes INTEGER,
                notes TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS medication_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                milligrams REAL,
                numberPerDose REAL,
                timesPerDay REAL,
                purchaseSource TEXT NOT NULL,
                pricePerBottle TEXT NOT NULL,
                numberPerBottle INTEGER,
                pricePerPill TEXT NOT NULL,
                purchasesPerYear REAL,
                notes TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS calorie_goal_changes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                changedDate TEXT NOT NULL,
                previousGoal INTEGER,
                newGoal INTEGER NOT NULL,
                reason TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
