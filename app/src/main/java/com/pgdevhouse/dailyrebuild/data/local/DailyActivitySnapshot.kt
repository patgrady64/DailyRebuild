package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A Daily Rebuild-owned snapshot of Health Connect totals saved automatically with the day.
 * This is separate from the source health data and can be deleted safely.
 */
@Entity(tableName = "daily_activity_snapshots")
data class DailyActivitySnapshot(
    @PrimaryKey
    val date: String,
    val steps: Long = 0L,
    val distanceMiles: Double = 0.0,
    val activityMinutes: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
