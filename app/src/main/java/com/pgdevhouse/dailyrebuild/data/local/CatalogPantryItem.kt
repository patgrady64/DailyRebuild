package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Exact current quantity for one imported catalog Product ID. */
@Entity(
    tableName = "catalog_pantry_items",
    foreignKeys = [
        ForeignKey(
            entity = CatalogProduct::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["usable"]),
        Index(value = ["storageLocation"])
    ]
)
data class CatalogPantryItem(
    @PrimaryKey
    val productId: String,
    val quantityOnHand: Double,
    val inventoryUnit: String,
    val usable: Boolean,
    val storageLocation: String? = null,
    val bestByEpochMillis: Long? = null,
    val lastCheckedEpochMillis: Long? = null,
    val notes: String? = null,
    val sourceFile: String,
    val sourceSheet: String,
    val sourceRow: Int,
    val importedAtEpochMillis: Long
)
