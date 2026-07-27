package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One completed or manually logged mobility session.
 *
 * Movement IDs are stored as pipe-delimited text so the first version can
 * preserve the exact generated routine without adding several relationship
 * tables. The movement library itself remains in MobilityUi.kt.
 */
@Entity(
    tableName = "mobility_sessions",
    indices = [
        Index(value = ["date"])
    ]
)
data class MobilitySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: String,
    val routineName: String,
    val plannedMovementIds: String,
    val completedMovementIds: String,
    val skippedMovementIds: String,
    val movementSeconds: Int,
    val elapsedSeconds: Int,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
)
