package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CareVisitDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlace(place: CarePlace): Long

    @Update
    suspend fun updatePlace(place: CarePlace)

    @Query(
        "SELECT * FROM care_places WHERE active = 1 " +
            "ORDER BY name COLLATE NOCASE ASC"
    )
    suspend fun getActivePlaces(): List<CarePlace>

    @Query("SELECT * FROM care_places WHERE id = :id LIMIT 1")
    suspend fun getPlaceById(id: Long): CarePlace?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProvider(provider: CareProvider): Long

    @Update
    suspend fun updateProvider(provider: CareProvider)

    @Query(
        "SELECT * FROM care_providers WHERE active = 1 " +
            "ORDER BY name COLLATE NOCASE ASC"
    )
    suspend fun getActiveProviders(): List<CareProvider>

    @Query(
        "SELECT * FROM care_providers WHERE placeId = :placeId AND active = 1 " +
            "ORDER BY name COLLATE NOCASE ASC"
    )
    suspend fun getProvidersForPlace(placeId: Long): List<CareProvider>

    @Query("SELECT * FROM care_providers WHERE id = :id LIMIT 1")
    suspend fun getProviderById(id: Long): CareProvider?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVisit(visit: CareVisit): Long

    @Update
    suspend fun updateVisit(visit: CareVisit)

    @Query("SELECT * FROM care_visits ORDER BY startedAt DESC")
    suspend fun getAllVisits(): List<CareVisit>

    @Query(
        "SELECT * FROM care_visits WHERE date BETWEEN :startDate AND :endDate " +
            "ORDER BY startedAt DESC"
    )
    suspend fun getVisitsBetween(
        startDate: String,
        endDate: String
    ): List<CareVisit>

    @Query(
        "SELECT * FROM care_visits WHERE date = :date " +
            "ORDER BY startedAt ASC"
    )
    suspend fun getVisitsForDate(date: String): List<CareVisit>

    @Query(
        "SELECT * FROM care_visits WHERE date = :date " +
            "AND placeName = :placeName " +
            "AND providerName = :providerName " +
            "AND ABS(startedAt - :startedAt) < 1800000 " +
            "AND id != :excludedId LIMIT 1"
    )
    suspend fun findPotentialDuplicate(
        date: String,
        placeName: String,
        providerName: String,
        startedAt: Long,
        excludedId: Long = 0L
    ): CareVisit?

    @Query("DELETE FROM care_visits WHERE id = :id")
    suspend fun deleteVisitById(id: Long)

    @Query("DELETE FROM care_visits WHERE date = :date")
    suspend fun deleteVisitsByDate(date: String)
}
