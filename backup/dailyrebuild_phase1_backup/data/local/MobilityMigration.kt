package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(
        database: SupportSQLiteDatabase
    ) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS mobility_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date TEXT NOT NULL,
                routineName TEXT NOT NULL,
                plannedMovementIds TEXT NOT NULL,
                completedMovementIds TEXT NOT NULL,
                skippedMovementIds TEXT NOT NULL,
                movementSeconds INTEGER NOT NULL,
                elapsedSeconds INTEGER NOT NULL,
                notes TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_mobility_sessions_date
            ON mobility_sessions(date)
            """.trimIndent()
        )
    }
}
