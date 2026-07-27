package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CareAppointmentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAppointment(appointment: CareAppointment): Long

    @Update
    suspend fun updateAppointment(appointment: CareAppointment)

    @Query("SELECT * FROM care_appointments ORDER BY scheduledAt DESC")
    suspend fun getAllAppointments(): List<CareAppointment>

    @Query(
        "SELECT * FROM care_appointments " +
            "WHERE scheduledAt >= :now " +
            "AND status IN ('Scheduled', 'Confirmed') " +
            "ORDER BY scheduledAt ASC"
    )
    suspend fun getUpcomingAppointments(now: Long): List<CareAppointment>

    @Query("SELECT * FROM care_appointments WHERE id = :id LIMIT 1")
    suspend fun getAppointmentById(id: Long): CareAppointment?

    @Query(
        "SELECT * FROM care_appointments WHERE date = :date " +
            "ORDER BY scheduledAt ASC"
    )
    suspend fun getAppointmentsForDate(date: String): List<CareAppointment>

    @Query(
        "SELECT * FROM care_appointments WHERE date = :date " +
            "AND placeName = :placeName " +
            "AND providerName = :providerName " +
            "AND ABS(scheduledAt - :scheduledAt) < 1800000 " +
            "AND id != :excludedId LIMIT 1"
    )
    suspend fun findPotentialDuplicate(
        date: String,
        placeName: String,
        providerName: String,
        scheduledAt: Long,
        excludedId: Long = 0L
    ): CareAppointment?

    @Query(
        "UPDATE care_appointments SET convertedVisitId = NULL, " +
            "updatedAt = :updatedAt WHERE convertedVisitId = :visitId"
    )
    suspend fun clearConvertedVisitLink(
        visitId: Long,
        updatedAt: Long
    )

    @Query("DELETE FROM care_appointments WHERE id = :id")
    suspend fun deleteAppointmentById(id: Long)

    @Query("DELETE FROM care_appointments WHERE date = :date")
    suspend fun deleteAppointmentsByDate(date: String)
}
