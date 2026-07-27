package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MigraineLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(log: MigraineLog): Long

    @Query(
        "SELECT * FROM migraine_logs " +
            "ORDER BY occurredAt DESC"
    )
    suspend fun getAllLogs(): List<MigraineLog>

    @Query(
        "SELECT * FROM migraine_logs " +
            "WHERE date = :date " +
            "ORDER BY occurredAt ASC"
    )
    suspend fun getLogsForDate(
        date: String
    ): List<MigraineLog>

    @Query(
        "SELECT * FROM migraine_logs " +
            "WHERE date BETWEEN :startDate AND :endDate " +
            "ORDER BY occurredAt ASC"
    )
    suspend fun getLogsBetween(
        startDate: String,
        endDate: String
    ): List<MigraineLog>

    @Query(
        "DELETE FROM migraine_logs " +
            "WHERE id = :id"
    )
    suspend fun deleteById(
        id: Long
    )

    @Query(
        "DELETE FROM migraine_logs " +
            "WHERE date = :date"
    )
    suspend fun deleteByDate(
        date: String
    )
}
