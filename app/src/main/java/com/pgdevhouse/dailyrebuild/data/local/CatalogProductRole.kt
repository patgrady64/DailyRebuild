package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** A reusable capability such as Sandwich Filling, Grain Base, or Snack. */
@Entity(
    tableName = "catalog_product_roles",
    primaryKeys = ["productId", "role"],
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
        Index(value = ["productId"]),
        Index(value = ["role", "active"])
    ]
)
data class CatalogProductRole(
    val productId: String,
    val role: String,
    val active: Boolean,
    val notes: String? = null,
    val sourceFile: String,
    val sourceSheet: String,
    val sourceRow: Int,
    val importedAtEpochMillis: Long
)
