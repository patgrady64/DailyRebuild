package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds prepared-food metadata, honest estimate labels, and reusable leftovers. */
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE `food_products` ADD COLUMN `sourceType` TEXT NOT NULL DEFAULT 'PACKAGED'"
        )
        database.execSQL(
            "ALTER TABLE `food_products` ADD COLUMN `sourceName` TEXT NOT NULL DEFAULT ''"
        )
        database.execSQL(
            "ALTER TABLE `food_products` ADD COLUMN `nutritionConfidence` TEXT NOT NULL DEFAULT 'EXACT'"
        )
        database.execSQL(
            "ALTER TABLE `food_products` ADD COLUMN `calorieEstimateLow` REAL"
        )
        database.execSQL(
            "ALTER TABLE `food_products` ADD COLUMN `calorieEstimateHigh` REAL"
        )
        database.execSQL(
            "ALTER TABLE `food_products` ADD COLUMN `nutritionNotes` TEXT NOT NULL DEFAULT ''"
        )
        database.execSQL(
            "ALTER TABLE `food_products` ADD COLUMN `isReusable` INTEGER NOT NULL DEFAULT 1"
        )

        database.execSQL(
            "ALTER TABLE `food_log_entries` ADD COLUMN `sourceTypeSnapshot` TEXT NOT NULL DEFAULT 'PACKAGED'"
        )
        database.execSQL(
            "ALTER TABLE `food_log_entries` ADD COLUMN `sourceNameSnapshot` TEXT NOT NULL DEFAULT ''"
        )
        database.execSQL(
            "ALTER TABLE `food_log_entries` ADD COLUMN `nutritionConfidenceSnapshot` TEXT NOT NULL DEFAULT 'EXACT'"
        )
        database.execSQL(
            "ALTER TABLE `food_log_entries` ADD COLUMN `calorieEstimateLow` REAL"
        )
        database.execSQL(
            "ALTER TABLE `food_log_entries` ADD COLUMN `calorieEstimateHigh` REAL"
        )
        database.execSQL(
            "ALTER TABLE `food_log_entries` ADD COLUMN `nutritionNotes` TEXT NOT NULL DEFAULT ''"
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `prepared_food_leftovers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `productId` INTEGER,
                `originEntryId` INTEGER,
                `originDate` TEXT NOT NULL,
                `sourceType` TEXT NOT NULL,
                `sourceName` TEXT NOT NULL,
                `foodName` TEXT NOT NULL,
                `portionUnit` TEXT NOT NULL,
                `remainingQuantity` REAL NOT NULL,
                `caloriesPerUnit` REAL NOT NULL,
                `proteinGramsPerUnit` REAL NOT NULL,
                `carbohydrateGramsPerUnit` REAL NOT NULL,
                `fatGramsPerUnit` REAL NOT NULL,
                `sodiumMilligramsPerUnit` REAL NOT NULL,
                `nutritionConfidence` TEXT NOT NULL,
                `calorieEstimateLowPerUnit` REAL,
                `calorieEstimateHighPerUnit` REAL,
                `notes` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_prepared_food_leftovers_productId` ON `prepared_food_leftovers` (`productId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_prepared_food_leftovers_originEntryId` ON `prepared_food_leftovers` (`originEntryId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_prepared_food_leftovers_originDate` ON `prepared_food_leftovers` (`originDate`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_prepared_food_leftovers_updatedAt` ON `prepared_food_leftovers` (`updatedAt`)"
        )
    }
}
