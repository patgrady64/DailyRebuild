package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One migraine or visual-aura event recorded by the user.
 *
 * Events are separate from DailyRecord because they are occasional health
 * events rather than daily goals. occurredAt stores the exact local event
 * instant as epoch milliseconds while date makes daily and weekly queries
 * straightforward.
 */
@Entity(
    tableName = "migraine_logs",
    indices = [Index(value = ["date"])]
)
data class MigraineLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: String,
    val occurredAt: Long,
    val auraDurationMinutes: Int? = null,
    val visualAura: Boolean = true,
    val headPain: Boolean = false,
    val foggyAfterward: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
