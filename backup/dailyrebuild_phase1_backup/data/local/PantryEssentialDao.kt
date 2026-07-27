package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PantryEssentialDao {
    @Query(
        """
        SELECT * FROM pantry_essentials
        ORDER BY
            CASE WHEN status = 'NEED' THEN 0 ELSE 1 END,
            category COLLATE NOCASE,
            name COLLATE NOCASE
        """
    )
    suspend fun getAll(): List<PantryEssential>

    @Query(
        """
        SELECT * FROM pantry_essentials
        WHERE status = 'NEED'
        ORDER BY category COLLATE NOCASE, name COLLATE NOCASE
        """
    )
    suspend fun getNeeded(): List<PantryEssential>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: PantryEssential): Long

    @Update
    suspend fun update(item: PantryEssential)

    @Delete
    suspend fun delete(item: PantryEssential)

    @Query(
        """
        UPDATE pantry_essentials
        SET status = :status, updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateStatus(
        id: Long,
        status: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE pantry_essentials
        SET status = 'HAVE', updatedAt = :updatedAt
        WHERE status = 'NEED'
        """
    )
    suspend fun markAllNeededAsHave(
        updatedAt: Long = System.currentTimeMillis()
    )
}
