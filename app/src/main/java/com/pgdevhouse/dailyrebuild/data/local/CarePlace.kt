package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A reusable medical, vision, dental, laboratory, imaging, therapy, or other
 * care location. Historical visits keep their own snapshot, so editing this
 * record later never rewrites older visit history.
 */
@Entity(
    tableName = "care_places",
    indices = [
        Index(value = ["name", "city", "state"]),
        Index(value = ["active"])
    ]
)
data class CarePlace(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val placeCategory: String = "Medical",
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String = "",
    val phone: String = "",
    val website: String = "",
    val patientPortal: String = "",
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
