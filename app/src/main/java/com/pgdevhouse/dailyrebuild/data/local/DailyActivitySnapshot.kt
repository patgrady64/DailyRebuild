package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A Daily Rebuild-owned snapshot of Health Connect totals at Save Today time.
 * This is separate from the source health data and can be deleted safely.
 */
@Entity(tableName = "daily_activity_snapshots")
data class DailyActivitySnapshot(
    @PrimaryKey
    val date: String,
    val steps: Long = 0L,
    val distanceMiles: Double = 0.0,
    val activeCalories: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)
