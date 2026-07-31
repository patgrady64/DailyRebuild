package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "iop_groups",
    indices = [
        Index(value = ["dayOfWeek", "startMinutes"]),
        Index(value = ["active"])
    ]
)
data class IopGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String = "IOP Group",
    val dayOfWeek: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val location: String = "",
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
