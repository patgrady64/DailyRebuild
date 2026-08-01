package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds condiment classification and optional saved-meal ingredients. */
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE `food_products` ADD COLUMN `isCondiment` INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE `saved_meal_ingredients` ADD COLUMN `isOptional` INTEGER NOT NULL DEFAULT 0"
        )
    }
}
