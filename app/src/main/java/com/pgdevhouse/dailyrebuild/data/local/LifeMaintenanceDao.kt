package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LifeMaintenanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(log: LifeMaintenanceLog)

    @Query(
        "SELECT * FROM life_maintenance_logs " +
            "ORDER BY date DESC, completedAt DESC"
    )
    suspend fun getAllLogs(): List<LifeMaintenanceLog>

    @Query(
        "SELECT * FROM life_maintenance_logs " +
            "WHERE date = :date " +
            "ORDER BY completedAt ASC"
    )
    suspend fun getLogsForDate(date: String): List<LifeMaintenanceLog>

    @Query(
        "DELETE FROM life_maintenance_logs " +
            "WHERE taskKey = :taskKey AND date = :date"
    )
    suspend fun delete(taskKey: String, date: String)

    @Query(
        "DELETE FROM life_maintenance_logs " +
            "WHERE date = :date"
    )
    suspend fun deleteByDate(date: String)
}
