package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One planned care appointment. Place and provider details are copied into the
 * appointment so editing the reusable directory does not silently rewrite an
 * existing plan.
 */
@Entity(
    tableName = "care_appointments",
    indices = [
        Index(value = ["date"]),
        Index(value = ["scheduledAt"]),
        Index(value = ["status"]),
        Index(value = ["placeId"]),
        Index(value = ["providerId"]),
        Index(value = ["convertedVisitId"])
    ]
)
data class CareAppointment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val placeId: Long? = null,
    val providerId: Long? = null,
    val date: String,
    val scheduledAt: Long,
    val status: String = "Scheduled",
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
    val reasonForAppointment: String = "",
    val transportationMode: String = "Not planned",
    val transportationDetails: String = "",
    val leaveByAt: Long? = null,
    val transportationConfirmed: Boolean = false,
    val questionsToAsk: String = "",
    val documentsToBring: String = "",
    val preparationNotes: String = "",
    val remindOneDayBefore: Boolean = true,
    val remindTwoHoursBefore: Boolean = true,
    val convertedVisitId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
