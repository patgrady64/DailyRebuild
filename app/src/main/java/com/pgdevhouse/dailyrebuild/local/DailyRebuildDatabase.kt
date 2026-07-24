package com.pgdevhouse.dailyrebuild.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DailyRebuildDatabase : RoomDatabase() {

    abstract fun dailyRecordDao(): DailyRecordDao

    companion object {

        @Volatile
        private var INSTANCE: DailyRebuildDatabase? = null

        fun getDatabase(
            context: Context
        ): DailyRebuildDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DailyRebuildDatabase::class.java,
                    "daily_rebuild_database"
                ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}