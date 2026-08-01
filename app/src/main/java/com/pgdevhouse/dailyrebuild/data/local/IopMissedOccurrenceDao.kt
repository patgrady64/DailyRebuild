package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IopMissedOccurrenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(value: IopMissedOccurrence): Long

    @Query(
        "SELECT * FROM iop_missed_occurrences " +
            "ORDER BY occurrenceDate DESC, startMinutesSnapshot DESC"
    )
    suspend fun getAll(): List<IopMissedOccurrence>

    @Query(
        "SELECT * FROM iop_missed_occurrences " +
            "WHERE occurrenceDate BETWEEN :startDate AND :endDate " +
            "ORDER BY occurrenceDate DESC, startMinutesSnapshot DESC"
    )
    suspend fun getBetween(
        startDate: String,
        endDate: String
    ): List<IopMissedOccurrence>

    @Query(
        "SELECT * FROM iop_missed_occurrences " +
            "WHERE groupId = :groupId AND occurrenceDate = :occurrenceDate " +
            "LIMIT 1"
    )
    suspend fun getForOccurrence(
        groupId: Long,
        occurrenceDate: String
    ): IopMissedOccurrence?

    @Query(
        "DELETE FROM iop_missed_occurrences " +
            "WHERE groupId = :groupId AND occurrenceDate = :occurrenceDate"
    )
    suspend fun deleteForOccurrence(
        groupId: Long,
        occurrenceDate: String
    )
}
