package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A reusable meeting location/profile.
 *
 * Attendance keeps a snapshot of the meeting details so editing this record
 * later never rewrites older attendance history.
 */
@Entity(
    tableName = "saved_meetings",
    indices = [
        Index(value = ["name", "city", "state"]),
        Index(value = ["active"])
    ]
)
data class SavedMeeting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String = "",
    val usualDayOfWeek: Int? = null,
    val usualStartMinutes: Int? = null,
    val typicalDurationMinutes: Int = 60,
    val favorite: Boolean = false,
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
