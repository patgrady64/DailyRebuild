package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface PreparedFoodLeftoverDao {
    @Insert
    suspend fun insert(leftover: PreparedFoodLeftover): Long

    @Update
    suspend fun update(leftover: PreparedFoodLeftover): Int

    @Upsert
    suspend fun save(leftover: PreparedFoodLeftover): Long

    @Delete
    suspend fun delete(leftover: PreparedFoodLeftover)

    @Query("SELECT * FROM prepared_food_leftovers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PreparedFoodLeftover?

    @Query("SELECT * FROM prepared_food_leftovers WHERE originEntryId = :entryId LIMIT 1")
    suspend fun getByOriginEntryId(entryId: Long): PreparedFoodLeftover?

    @Query("DELETE FROM prepared_food_leftovers WHERE originEntryId = :entryId")
    suspend fun deleteByOriginEntryId(entryId: Long): Int

    @Query("DELETE FROM prepared_food_leftovers WHERE originDate = :date")
    suspend fun deleteByOriginDate(date: String): Int

    @Query(
        """
        SELECT * FROM prepared_food_leftovers
        WHERE remainingQuantity > 0.0001
        ORDER BY updatedAt DESC, id DESC
        """
    )
    suspend fun getAvailable(): List<PreparedFoodLeftover>

    @Query(
        """
        SELECT * FROM prepared_food_leftovers
        ORDER BY updatedAt DESC, id DESC
        """
    )
    suspend fun getAll(): List<PreparedFoodLeftover>
}
