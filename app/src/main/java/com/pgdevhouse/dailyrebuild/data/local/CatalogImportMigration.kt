package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds normalized catalog, product-role, and exact pantry tables. */
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `catalog_products` (
                `productId` TEXT NOT NULL,
                `productType` TEXT NOT NULL,
                `active` INTEGER NOT NULL,
                `genericName` TEXT NOT NULL,
                `productName` TEXT NOT NULL,
                `flavorVariant` TEXT,
                `brand` TEXT,
                `category` TEXT,
                `walmartUrl` TEXT,
                `snapEbtEligible` INTEGER,
                `shelfStable` INTEGER,
                `storage` TEXT,
                `microwave` TEXT,
                `openedPackageRule` TEXT,
                `packageSize` TEXT,
                `packagePrice` REAL NOT NULL,
                `labelServingsPerPackage` REAL,
                `planningUnit` TEXT NOT NULL,
                `planningUnitsPerLabelServing` REAL NOT NULL,
                `planningUnitsPerPackage` REAL NOT NULL,
                `caloriesPerPlanningUnit` REAL NOT NULL,
                `proteinGramsPerPlanningUnit` REAL,
                `carbohydrateGramsPerPlanningUnit` REAL,
                `fiberGramsPerPlanningUnit` REAL,
                `sodiumMilligramsPerPlanningUnit` REAL,
                `totalFatGramsPerPlanningUnit` REAL,
                `saturatedFatGramsPerPlanningUnit` REAL,
                `addedSugarGramsPerPlanningUnit` REAL,
                `notes` TEXT,
                `lastPriceCheckedEpochMillis` INTEGER,
                `sourceFile` TEXT NOT NULL,
                `sourceSheet` TEXT NOT NULL,
                `sourceRow` INTEGER NOT NULL,
                `importedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`productId`)
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_catalog_products_productType_active` " +
                "ON `catalog_products` (`productType`, `active`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_catalog_products_category` " +
                "ON `catalog_products` (`category`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_catalog_products_productName` " +
                "ON `catalog_products` (`productName`)"
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `catalog_product_roles` (
                `productId` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                `active` INTEGER NOT NULL,
                `notes` TEXT,
                `sourceFile` TEXT NOT NULL,
                `sourceSheet` TEXT NOT NULL,
                `sourceRow` INTEGER NOT NULL,
                `importedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`productId`, `role`),
                FOREIGN KEY(`productId`) REFERENCES `catalog_products`(`productId`)
                    ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_catalog_product_roles_productId` " +
                "ON `catalog_product_roles` (`productId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_catalog_product_roles_role_active` " +
                "ON `catalog_product_roles` (`role`, `active`)"
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `catalog_pantry_items` (
                `productId` TEXT NOT NULL,
                `quantityOnHand` REAL NOT NULL,
                `inventoryUnit` TEXT NOT NULL,
                `usable` INTEGER NOT NULL,
                `storageLocation` TEXT,
                `bestByEpochMillis` INTEGER,
                `lastCheckedEpochMillis` INTEGER,
                `notes` TEXT,
                `sourceFile` TEXT NOT NULL,
                `sourceSheet` TEXT NOT NULL,
                `sourceRow` INTEGER NOT NULL,
                `importedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`productId`),
                FOREIGN KEY(`productId`) REFERENCES `catalog_products`(`productId`)
                    ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_catalog_pantry_items_usable` " +
                "ON `catalog_pantry_items` (`usable`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_catalog_pantry_items_storageLocation` " +
                "ON `catalog_pantry_items` (`storageLocation`)"
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `catalog_import_sources` (
                `fileName` TEXT NOT NULL,
                `sha256` TEXT NOT NULL,
                `lastModifiedEpochMillis` INTEGER NOT NULL,
                `importedSectionsJson` TEXT NOT NULL,
                `exportedAtEpochMillis` INTEGER NOT NULL,
                `importedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`fileName`)
            )
            """.trimIndent()
        )
    }
}
