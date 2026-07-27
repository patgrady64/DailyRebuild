package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object PantryEssentialStatus {
    const val HAVE = "HAVE"
    const val NEED = "NEED"
}

@Entity(
    tableName = "pantry_essentials",
    indices = [
        Index(value = ["status"]),
        Index(value = ["name"])
    ]
)
data class PantryEssential(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val category: String = "Condiments",
    val status: String = PantryEssentialStatus.HAVE,
    val preferredProduct: String = "",
    val brandPreference: String = "Any brand",
    val expectedPrice: Double? = null,
    val walmartUrl: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isNeeded: Boolean
        get() = status == PantryEssentialStatus.NEED
}
