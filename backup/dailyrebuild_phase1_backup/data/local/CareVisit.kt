package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One completed care visit. Place and provider details are copied into this
 * row so future edits to the reusable directory do not change history.
 */
@Entity(
    tableName = "care_visits",
    indices = [
        Index(value = ["date"]),
        Index(value = ["startedAt"]),
        Index(value = ["placeId"]),
        Index(value = ["providerId"]),
        Index(value = ["visitCategory"])
    ]
)
data class CareVisit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val placeId: Long? = null,
    val providerId: Long? = null,
    val date: String,
    val startedAt: Long,
    val visitCategory: String,
    val visitFormat: String = "In person",
    val placeName: String,
    val placeCategory: String = "",
    val providerName: String = "",
    val providerCredentials: String = "",
    val providerSpecialty: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val placePhone: String = "",
    val providerPhone: String = "",
    val reasonForVisit: String,
    val visitSummary: String = "",
    val testsProcedures: String = "",
    val resultsDiscussed: String = "",
    val instructions: String = "",
    val medicationChanges: String = "",
    val referrals: String = "",
    val followUpDate: String? = null,
    val notes: String = "",
    val weightPounds: Double? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val a1c: Double? = null,
    val bloodGlucose: Double? = null,
    val cholesterolTotal: Double? = null,
    val cholesterolLdl: Double? = null,
    val cholesterolHdl: Double? = null,
    val triglycerides: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
