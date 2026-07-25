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

    @Delete
    suspend fun deleteSnapshot(
        snapshot: DailyActivitySnapshot
    )
}
