package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DrinkDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDefinition(definition: DrinkDefinition): Long

    @Update
    suspend fun updateDefinition(definition: DrinkDefinition)

    @Delete
    suspend fun deleteDefinition(definition: DrinkDefinition)

    @Query("SELECT * FROM drink_definitions WHERE isActive = 1 ORDER BY isFavorite DESC, updatedAt DESC, name COLLATE NOCASE ASC")
    suspend fun getActiveDefinitions(): List<DrinkDefinition>

    @Query("SELECT * FROM drink_definitions ORDER BY isActive DESC, isFavorite DESC, name COLLATE NOCASE ASC")
    suspend fun getAllDefinitions(): List<DrinkDefinition>

    @Query("SELECT * FROM drink_definitions WHERE id = :id LIMIT 1")
    suspend fun getDefinitionById(id: Long): DrinkDefinition?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: DrinkEntry): Long

    @Update
    suspend fun updateEntry(entry: DrinkEntry)

    @Delete
    suspend fun deleteEntry(entry: DrinkEntry)

    @Query("DELETE FROM drink_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("DELETE FROM drink_entries WHERE date = :date")
    suspend fun deleteEntriesForDate(date: String)

    @Query("SELECT * FROM drink_entries WHERE date = :date ORDER BY consumedAt ASC, id ASC")
    suspend fun getEntriesForDate(date: String): List<DrinkEntry>

    @Query("SELECT * FROM drink_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY consumedAt ASC, id ASC")
    suspend fun getEntriesBetween(startDate: String, endDate: String): List<DrinkEntry>

    @Query("SELECT * FROM drink_entries ORDER BY consumedAt DESC, id DESC")
    suspend fun getAllEntries(): List<DrinkEntry>

    @Query(
        "SELECT * FROM drink_entries WHERE definitionId = :definitionId " +
            "ORDER BY consumedAt DESC, id DESC LIMIT :limit"
    )
    suspend fun getRecentEntriesForDefinition(
        definitionId: Long,
        limit: Int = 10
    ): List<DrinkEntry>
}
