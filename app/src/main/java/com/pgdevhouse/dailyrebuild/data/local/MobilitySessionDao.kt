package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MobilitySessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSession(
        session: MobilitySession
    ): Long

    @Query(
        """
        SELECT *
        FROM mobility_sessions
        WHERE date = :date
        ORDER BY createdAt DESC
        """
    )
    suspend fun getSessionsForDate(
        date: String
    ): List<MobilitySession>

    @Query(
        """
        SELECT *
        FROM mobility_sessions
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date ASC, createdAt ASC
        """
    )
    suspend fun getSessionsBetween(
        startDate: String,
        endDate: String
    ): List<MobilitySession>

    @Query(
        """
        SELECT *
        FROM mobility_sessions
        ORDER BY date ASC, createdAt ASC
        """
    )
    suspend fun getAllSessions(): List<MobilitySession>

    @Delete
    suspend fun deleteSession(
        session: MobilitySession
    )

    @Query(
        """
        DELETE FROM mobility_sessions
        WHERE date = :date
        """
    )
    suspend fun deleteSessionsForDate(
        date: String
    )
}
