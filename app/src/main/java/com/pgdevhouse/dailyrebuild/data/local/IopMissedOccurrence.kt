package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An exception to Daily Rebuild's default IOP attendance rule.
 *
 * A scheduled IOP occurrence is treated as attended automatically once it has
 * ended. A row exists here only when the user explicitly marks that occurrence
 * missed and records a reason.
 */
@Entity(
    tableName = "iop_missed_occurrences",
    indices = [
        Index(value = ["groupId", "occurrenceDate"], unique = true),
        Index(value = ["occurrenceDate"])
    ]
)
data class IopMissedOccurrence(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val groupId: Long,
    val occurrenceDate: String,
    val groupNameSnapshot: String,
    val startMinutesSnapshot: Int,
    val endMinutesSnapshot: Int,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
