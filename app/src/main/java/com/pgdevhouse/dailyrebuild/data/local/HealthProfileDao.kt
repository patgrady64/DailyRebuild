package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HealthProfileDao {

    @Query("SELECT * FROM health_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): HealthProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: HealthProfile)

    @Query(
        "SELECT * FROM health_measurements " +
            "ORDER BY recordedDate DESC, createdAt DESC"
    )
    suspend fun getAllMeasurements(): List<HealthMeasurement>

    @Query(
        "SELECT * FROM health_measurements WHERE type = :type " +
            "ORDER BY recordedDate DESC, createdAt DESC"
    )
    suspend fun getMeasurementsByType(
        type: String
    ): List<HealthMeasurement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMeasurement(
        measurement: HealthMeasurement
    ): Long

    @Delete
    suspend fun deleteMeasurement(
        measurement: HealthMeasurement
    )

    @Query(
        "SELECT * FROM pain_activity_logs " +
            "ORDER BY recordedDate DESC, createdAt DESC"
    )
    suspend fun getPainActivityLogs(): List<PainActivityLog>

    @Insert
    suspend fun addPainActivityLog(
        log: PainActivityLog
    ): Long

    @Delete
    suspend fun deletePainActivityLog(
        log: PainActivityLog
    )

    @Query(
        "SELECT * FROM medication_entries " +
            "ORDER BY sortOrder ASC, name COLLATE NOCASE ASC"
    )
    suspend fun getMedications(): List<MedicationEntry>

    @Query("SELECT COUNT(*) FROM medication_entries")
    suspend fun countMedications(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMedication(
        medication: MedicationEntry
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMedications(
        medications: List<MedicationEntry>
    )

    @Delete
    suspend fun deleteMedication(
        medication: MedicationEntry
    )

    @Query(
        "SELECT * FROM calorie_goal_changes " +
            "ORDER BY changedDate DESC, createdAt DESC"
    )
    suspend fun getCalorieGoalChanges(): List<CalorieGoalChange>

    @Insert
    suspend fun addCalorieGoalChange(
        change: CalorieGoalChange
    ): Long
}
