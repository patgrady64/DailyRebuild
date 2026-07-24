package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DailyRecordDao {

    @Query(
        """
        SELECT *
        FROM daily_records
        WHERE date = :date
        LIMIT 1
        """
    )
    suspend fun getRecordByDate(
        date: String
    ): DailyRecord?

    /*
     * If the date does not exist, Room inserts it.
     * If it already exists, Room updates it.
     */
    @Upsert
    suspend fun saveRecord(
        record: DailyRecord
    )

    /*
     * This will later support the History and Export screens.
     */
    @Query(
        """
        SELECT *
        FROM daily_records
        ORDER BY date DESC
        """
    )
    suspend fun getAllRecords(): List<DailyRecord>

    @androidx.room.Delete
    suspend fun deleteRecord(
        record: DailyRecord
    )
}