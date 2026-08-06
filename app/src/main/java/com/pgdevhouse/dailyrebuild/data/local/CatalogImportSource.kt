package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Source workbook fingerprint recorded with the latest successful import. */
@Entity(tableName = "catalog_import_sources")
data class CatalogImportSource(
    @PrimaryKey
    val fileName: String,
    val sha256: String,
    val lastModifiedEpochMillis: Long,
    val importedSectionsJson: String,
    val exportedAtEpochMillis: Long,
    val importedAtEpochMillis: Long
)
