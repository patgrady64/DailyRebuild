package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_entries")
data class MedicationEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val milligrams: Double? = null,
    val numberPerDose: Double? = null,
    val timesPerDay: Double? = null,
    val purchaseSource: String = "",
    val pricePerBottle: String = "",
    val numberPerBottle: Int? = null,
    val pricePerPill: String = "",
    val purchasesPerYear: Double? = null,
    val notes: String = "",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)
