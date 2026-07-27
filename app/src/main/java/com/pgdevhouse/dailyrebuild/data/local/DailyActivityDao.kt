package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSnapshot(
        snapshot: DailyActivitySnapshot
    )

    @Query(
        """
        SELECT * FROM daily_activity_snapshots
        WHERE date = :date
        LIMIT 1
        """
    )
    suspend fun getSnapshotByDate(
        date: String
    ): DailyActivitySnapshot?

    @Query(
        """
        SELECT * FROM daily_activity_snapshots
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date ASC
        """
    )
    suspend fun getSnapshotsBetween(
        startDate: String,
        endDate: String
    ): List<DailyActivitySnapshot>

    @Query(
        """
        SELECT * FROM daily_activity_snapshots
        ORDER BY date ASC
        """
    )
    suspend fun getAllSnapshots(): List<DailyActivitySnapshot>

    @Delete
    suspend fun deleteSnapshot(
        snapshot: DailyActivitySnapshot
    )
}
