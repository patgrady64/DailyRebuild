package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object HealthMeasurementType {
    const val WEIGHT = "WEIGHT"
    const val A1C = "A1C"
    const val BLOOD_PRESSURE = "BLOOD_PRESSURE"
    const val CHOLESTEROL = "CHOLESTEROL"
}

@Entity(
    tableName = "health_measurements",
    indices = [
        Index(
            value = ["recordedDate", "type"],
            unique = true
        )
    ]
)
data class HealthMeasurement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val recordedDate: String,
    val type: String,
    val primaryValue: Double,
    val secondaryValue: Double? = null,
    val tertiaryValue: Double? = null,
    val quaternaryValue: Double? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
