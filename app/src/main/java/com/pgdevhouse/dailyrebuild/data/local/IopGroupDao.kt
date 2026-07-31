package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface IopGroupDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(group: IopGroup): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(groups: List<IopGroup>): List<Long>

    @Update
    suspend fun update(group: IopGroup)

    @Delete
    suspend fun delete(group: IopGroup)

    @Query(
        "SELECT * FROM iop_groups " +
            "ORDER BY dayOfWeek ASC, startMinutes ASC, name COLLATE NOCASE ASC"
    )
    suspend fun getAll(): List<IopGroup>

    @Query(
        "SELECT * FROM iop_groups " +
            "WHERE active = 1 " +
            "ORDER BY dayOfWeek ASC, startMinutes ASC, name COLLATE NOCASE ASC"
    )
    suspend fun getActive(): List<IopGroup>
}
