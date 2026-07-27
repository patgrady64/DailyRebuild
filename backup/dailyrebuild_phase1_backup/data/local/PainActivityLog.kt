package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pain_activity_logs")
data class PainActivityLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val recordedDate: String,
    val activityType: String,
    val bodyArea: String,
    val painBefore: Int,
    val painAfter: Int,
    val durationMinutes: Int? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
