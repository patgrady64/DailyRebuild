package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds stable saved-meal identity and an accumulated meal quantity to each
 * ingredient snapshot in a logged meal group.
 */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE food_log_entries ADD COLUMN savedMealId INTEGER"
        )
        database.execSQL(
            "ALTER TABLE food_log_entries " +
                "ADD COLUMN mealQuantity REAL NOT NULL DEFAULT 1.0"
        )
    }
}
