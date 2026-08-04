package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds reusable beverages and timestamped drink history. */

private data class LegacyWaterImport(
    val column: String,
    val name: String,
    val category: String,
    val amount: Double,
    val timeOffset: Int
)

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `drink_definitions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `defaultAmountFlOz` REAL NOT NULL,
                `caloriesPerDefaultAmount` REAL NOT NULL,
                `carbohydrateGramsPerDefaultAmount` REAL NOT NULL,
                `sugarGramsPerDefaultAmount` REAL NOT NULL,
                `proteinGramsPerDefaultAmount` REAL NOT NULL,
                `caffeineMilligramsPerDefaultAmount` REAL NOT NULL,
                `countsAsWater` INTEGER NOT NULL,
                `countsAsFood` INTEGER NOT NULL,
                `containerName` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `isFavorite` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_drink_definitions_name` ON `drink_definitions` (`name`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_drink_definitions_isFavorite_isActive` ON `drink_definitions` (`isFavorite`, `isActive`)")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `drink_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` TEXT NOT NULL,
                `consumedAt` INTEGER NOT NULL,
                `definitionId` INTEGER,
                `drinkNameSnapshot` TEXT NOT NULL,
                `categorySnapshot` TEXT NOT NULL,
                `amountFlOz` REAL NOT NULL,
                `calories` REAL NOT NULL,
                `carbohydrateGrams` REAL NOT NULL,
                `sugarGrams` REAL NOT NULL,
                `proteinGrams` REAL NOT NULL,
                `caffeineMilligrams` REAL NOT NULL,
                `countsAsWater` INTEGER NOT NULL,
                `countsAsFood` INTEGER NOT NULL,
                `notes` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY (`definitionId`) REFERENCES `drink_definitions` (`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_drink_entries_date` ON `drink_entries` (`date`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_drink_entries_definitionId` ON `drink_entries` (`definitionId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_drink_entries_consumedAt` ON `drink_entries` (`consumedAt`)")

        val now = System.currentTimeMillis()
        seedDefinition(database, "Water", DrinkCategory.WATER, 24.0, true, false, "Reusable bottle", true, now)
        seedDefinition(database, "MiO / flavored water", DrinkCategory.FLAVORED_WATER, 24.0, true, false, "Reusable bottle", true, now)
        seedDefinition(database, "Coffee", DrinkCategory.COFFEE, 12.0, false, false, "Coffee mug", true, now)
        seedDefinition(database, "Tea", DrinkCategory.TEA, 12.0, false, false, "Mug", false, now)
        seedDefinition(database, "Soda", DrinkCategory.SODA, 12.0, false, false, "Can", false, now)
        seedDefinition(database, "Diet soda", DrinkCategory.DIET_SODA, 12.0, false, false, "Can", false, now)
        seedDefinition(database, "Juice", DrinkCategory.JUICE, 8.0, false, false, "Glass", false, now)
        seedDefinition(database, "Milk", DrinkCategory.MILK, 8.0, false, false, "Glass", false, now)
        seedDefinition(database, "Protein drink", DrinkCategory.PROTEIN_DRINK, 11.0, false, true, "Bottle", false, now)
        seedDefinition(database, "Sports drink", DrinkCategory.SPORTS_DRINK, 20.0, false, false, "Bottle", false, now)
        seedDefinition(database, "Energy drink", DrinkCategory.ENERGY_DRINK, 12.0, false, false, "Can", false, now)

        // Preserve every historical bottle count. Old records are copied into
        // aggregate imported entries; the original columns remain for backups
        // from older versions and as a safe fallback.
        importLegacyWater(database, now)
        database.execSQL(
            "UPDATE `daily_records` SET " +
                "`plainReusableBottleCount` = 0, " +
                "`mioReusableBottleCount` = 0, " +
                "`plainDisposableBottleCount` = 0, " +
                "`mioDisposableBottleCount` = 0"
        )
    }

    private fun seedDefinition(
        database: SupportSQLiteDatabase,
        name: String,
        category: String,
        amount: Double,
        countsAsWater: Boolean,
        countsAsFood: Boolean,
        container: String,
        favorite: Boolean,
        now: Long
    ) {
        database.execSQL(
            """
            INSERT INTO `drink_definitions` (
                `name`, `category`, `defaultAmountFlOz`,
                `caloriesPerDefaultAmount`, `carbohydrateGramsPerDefaultAmount`,
                `sugarGramsPerDefaultAmount`, `proteinGramsPerDefaultAmount`,
                `caffeineMilligramsPerDefaultAmount`, `countsAsWater`,
                `countsAsFood`, `containerName`, `notes`, `isFavorite`,
                `isActive`, `createdAt`, `updatedAt`
            ) VALUES (?, ?, ?, 0, 0, 0, 0, 0, ?, ?, ?, '', ?, 1, ?, ?)
            """.trimIndent(),
            arrayOf(name, category, amount, if (countsAsWater) 1 else 0, if (countsAsFood) 1 else 0, container, if (favorite) 1 else 0, now, now)
        )
    }

    private fun importLegacyWater(database: SupportSQLiteDatabase, now: Long) {
        val rows = listOf(
            LegacyWaterImport("plainReusableBottleCount", "Imported plain water", DrinkCategory.WATER, 24.0, 1),
            LegacyWaterImport("mioReusableBottleCount", "Imported MiO water", DrinkCategory.FLAVORED_WATER, 24.0, 1),
            LegacyWaterImport("plainDisposableBottleCount", "Imported plain water", DrinkCategory.WATER, 16.9, 2),
            LegacyWaterImport("mioDisposableBottleCount", "Imported MiO water", DrinkCategory.FLAVORED_WATER, 16.9, 2)
        )
        rows.forEach { row ->
            database.execSQL(
                """
                INSERT INTO `drink_entries` (
                    `date`, `consumedAt`, `definitionId`, `drinkNameSnapshot`,
                    `categorySnapshot`, `amountFlOz`, `calories`,
                    `carbohydrateGrams`, `sugarGrams`, `proteinGrams`,
                    `caffeineMilligrams`, `countsAsWater`, `countsAsFood`,
                    `notes`, `createdAt`, `updatedAt`
                )
                SELECT `date`, `updatedAt` + ${row.timeOffset}, NULL, ?, ?,
                    `${row.column}` * ${row.amount}, 0, 0, 0, 0, 0, 1, 0,
                    'Imported from the original water bottle counter.', ?, ?
                FROM `daily_records`
                WHERE `${row.column}` > 0
                """.trimIndent(),
                arrayOf(row.name, row.category, now, now)
            )
        }
    }
}
