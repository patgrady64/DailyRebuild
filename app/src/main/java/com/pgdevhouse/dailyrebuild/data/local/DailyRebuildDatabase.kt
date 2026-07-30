package com.pgdevhouse.dailyrebuild.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DailyRecord::class,
        FoodProduct::class,
        FoodLogEntry::class,
        SavedMeal::class,
        SavedMealIngredient::class,
        DailyActivitySnapshot::class,
        MobilitySession::class,
        HealthProfile::class,
        HealthMeasurement::class,
        PainActivityLog::class,
        MedicationEntry::class,
        CalorieGoalChange::class,
        ShowerLog::class,
        MigraineLog::class,
        SavedMeeting::class,
        MeetingAttendance::class,
        CarePlace::class,
        CareProvider::class,
        CareVisit::class,
        CareAppointment::class,
        PantryEssential::class,
        LifeMaintenanceLog::class,
    ],
    version = 15,
    exportSchema = false
)
abstract class DailyRebuildDatabase :
    RoomDatabase() {

    abstract fun dailyRecordDao():
            DailyRecordDao

    abstract fun foodDao():
            FoodDao

    abstract fun mealDao():
            MealDao

    companion object {

        @Volatile
        private var INSTANCE:
                DailyRebuildDatabase? = null

        /*
         * Version 1 contained only daily_records.
         *
         * Version 2 adds:
         *
         * food_products
         * food_log_entries
         */
        private val MIGRATION_1_2 =
            object : Migration(1, 2) {

                override fun migrate(
                    database:
                    SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `food_products`
                        (
                            `id` INTEGER PRIMARY KEY
                                AUTOINCREMENT NOT NULL,

                            `barcode` TEXT,

                            `name` TEXT NOT NULL,

                            `brand` TEXT NOT NULL,

                            `caloriesPerServing`
                                REAL NOT NULL,

                            `proteinGramsPerServing`
                                REAL NOT NULL,

                            `carbohydrateGramsPerServing`
                                REAL NOT NULL,

                            `fatGramsPerServing`
                                REAL NOT NULL,

                            `sodiumMilligramsPerServing`
                                REAL NOT NULL,

                            `servingQuantity`
                                REAL NOT NULL,

                            `servingUnit`
                                TEXT NOT NULL,

                            `packageQuantity`
                                REAL,

                            `packageUnit`
                                TEXT,

                            `isFavorite`
                                INTEGER NOT NULL,

                            `createdAt`
                                INTEGER NOT NULL,

                            `updatedAt`
                                INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE UNIQUE INDEX
                        IF NOT EXISTS
                        `index_food_products_barcode`
                        ON `food_products` (`barcode`)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS
                        `food_log_entries`
                        (
                            `id` INTEGER PRIMARY KEY
                                AUTOINCREMENT NOT NULL,

                            `date` TEXT NOT NULL,

                            `productId`
                                INTEGER NOT NULL,

                            `quantity`
                                REAL NOT NULL,

                            `unit`
                                TEXT NOT NULL,

                            `mealName`
                                TEXT,

                            `productNameSnapshot`
                                TEXT NOT NULL,

                            `calories`
                                REAL NOT NULL,

                            `proteinGrams`
                                REAL NOT NULL,

                            `carbohydrateGrams`
                                REAL NOT NULL,

                            `fatGrams`
                                REAL NOT NULL,

                            `sodiumMilligrams`
                                REAL NOT NULL,

                            `createdAt`
                                INTEGER NOT NULL,

                            FOREIGN KEY (`productId`)
                                REFERENCES
                                `food_products` (`id`)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_log_entries_date`
                        ON `food_log_entries` (`date`)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        `index_food_log_entries_productId`
                        ON `food_log_entries`
                        (`productId`)
                        """.trimIndent()
                    )
                }
            }

        fun getDatabase(
            context: Context
        ): DailyRebuildDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    val instance =
                        Room.databaseBuilder(
                            context.applicationContext,
                            DailyRebuildDatabase::class.java,
                            "daily_rebuild_database"
                        )
                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4,
                                MIGRATION_4_5,
                                MIGRATION_5_6,
                                MIGRATION_6_7,
                                MIGRATION_7_8,
                                MIGRATION_8_9,
                                MIGRATION_9_10,
                                MIGRATION_10_11,
                                MIGRATION_11_12,
                                MIGRATION_12_13,
                                MIGRATION_13_14,
                                MIGRATION_14_15
                            )
                            .build()

                    INSTANCE = instance

                    instance
                }
        }

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {

                override fun migrate(
                    database:
                    SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                CREATE TABLE IF NOT EXISTS
                `saved_meals`
                (
                    `id` INTEGER PRIMARY KEY
                        AUTOINCREMENT NOT NULL,

                    `name` TEXT NOT NULL,

                    `isFavorite`
                        INTEGER NOT NULL,

                    `createdAt`
                        INTEGER NOT NULL,

                    `updatedAt`
                        INTEGER NOT NULL
                )
                """.trimIndent()
                    )

                    database.execSQL(
                        """
                CREATE TABLE IF NOT EXISTS
                `saved_meal_ingredients`
                (
                    `id` INTEGER PRIMARY KEY
                        AUTOINCREMENT NOT NULL,

                    `mealId`
                        INTEGER NOT NULL,

                    `productId`
                        INTEGER NOT NULL,

                    `amountMode`
                        TEXT NOT NULL,

                    `amount`
                        REAL NOT NULL,

                    `sortOrder`
                        INTEGER NOT NULL,

                    FOREIGN KEY (`mealId`)
                        REFERENCES
                        `saved_meals` (`id`)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE,

                    FOREIGN KEY (`productId`)
                        REFERENCES
                        `food_products` (`id`)
                        ON UPDATE NO ACTION
                        ON DELETE NO ACTION
                )
                """.trimIndent()
                    )

                    database.execSQL(
                        """
                CREATE INDEX IF NOT EXISTS
                `index_saved_meal_ingredients_mealId`
                ON `saved_meal_ingredients`
                (`mealId`)
                """.trimIndent()
                    )

                    database.execSQL(
                        """
                CREATE INDEX IF NOT EXISTS
                `index_saved_meal_ingredients_productId`
                ON `saved_meal_ingredients`
                (`productId`)
                """.trimIndent()
                    )
                }
            }

        val MIGRATION_3_4 = object : Migration(3, 4) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {
                database.execSQL(
                    "ALTER TABLE food_log_entries " +
                            "ADD COLUMN mealLogId TEXT"
                )
            }
        }

    }
    abstract fun dailyActivityDao(): DailyActivityDao

    abstract fun mobilitySessionDao(): MobilitySessionDao

    abstract fun healthProfileDao(): HealthProfileDao

    abstract fun showerLogDao(): ShowerLogDao

    abstract fun migraineLogDao(): MigraineLogDao

    abstract fun meetingDao(): MeetingDao

    abstract fun careVisitDao(): CareVisitDao

    abstract fun careAppointmentDao(): CareAppointmentDao

    abstract fun pantryEssentialDao(): PantryEssentialDao

    abstract fun lifeMaintenanceDao(): LifeMaintenanceDao
}