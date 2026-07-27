package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A reusable doctor, dentist, optometrist, therapist, nurse practitioner, or
 * other provider associated with one saved place in the first version.
 */
@Entity(
    tableName = "care_providers",
    indices = [
        Index(value = ["placeId"]),
        Index(value = ["name"]),
        Index(value = ["active"])
    ]
)
data class CareProvider(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val placeId: Long,
    val name: String,
    val credentials: String = "",
    val specialty: String = "",
    val phone: String = "",
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
