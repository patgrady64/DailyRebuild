package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One meeting attendance event.
 *
 * savedMeetingId is nullable so a one-time meeting can be logged without
 * creating a permanent SavedMeeting. The visible meeting information is
 * copied into this row to preserve an accurate historical snapshot.
 */
@Entity(
    tableName = "meeting_attendance",
    indices = [
        Index(value = ["date"]),
        Index(value = ["savedMeetingId"]),
        Index(value = ["startedAt"])
    ]
)
data class MeetingAttendance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val savedMeetingId: Long? = null,
    val date: String,
    val startedAt: Long,
    val durationMinutes: Int = 60,
    val meetingName: String,
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val role: String = "Attended",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
