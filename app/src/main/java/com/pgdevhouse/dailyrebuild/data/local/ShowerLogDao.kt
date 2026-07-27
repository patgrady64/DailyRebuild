package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ShowerLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(log: ShowerLog)

    @Query(
        "SELECT * FROM shower_logs " +
            "WHERE date = :date LIMIT 1"
    )
    suspend fun getLogByDate(
        date: String
    ): ShowerLog?

    @Query(
        "SELECT * FROM shower_logs " +
            "WHERE date BETWEEN :startDate AND :endDate " +
            "ORDER BY date ASC"
    )
    suspend fun getLogsBetween(
        startDate: String,
        endDate: String
    ): List<ShowerLog>

    @Query(
        "SELECT * FROM shower_logs ORDER BY date ASC"
    )
    suspend fun getAllLogs(): List<ShowerLog>

    @Query(
        "DELETE FROM shower_logs " +
            "WHERE date = :date"
    )
    suspend fun deleteByDate(
        date: String
    )
}
