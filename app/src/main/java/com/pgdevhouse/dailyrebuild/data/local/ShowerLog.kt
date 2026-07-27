package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One shower completion for one local calendar date.
 *
 * Showering is intentionally separate from DailyRecord because it is a
 * weekly habit rather than a daily anchor. The date primary key prevents
 * accidental duplicate counts when the user taps Log more than once.
 */
@Entity(tableName = "shower_logs")
data class ShowerLog(
    @PrimaryKey
    val date: String,
    val completedAt: Long = System.currentTimeMillis()
)
