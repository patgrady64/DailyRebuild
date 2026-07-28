package com.pgdevhouse.dailyrebuild.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.room.withTransaction
import com.pgdevhouse.dailyrebuild.data.local.DailyRebuildDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Exports and restores every Daily Rebuild-owned Room table.
 *
 * The backup is intentionally JSON inside a ZIP instead of a raw SQLite copy.
 * That makes the file inspectable and gives future app versions a path to add
 * format migrations without depending on SQLite WAL or journal sidecar files.
 */
class DailyRebuildBackupManager(
    private val context: Context,
    private val database: DailyRebuildDatabase
) {
    data class BackupSummary(
        val createdAtEpochMillis: Long,
        val appVersionName: String,
        val databaseVersion: Int,
        val totalRecords: Int,
        val tableCounts: Map<String, Int>
    )

    data class BackupInspection(
        val summary: BackupSummary,
        val sourceLabel: String
    )

    data class RestoreResult(
        val summary: BackupSummary,
        val emergencyBackup: EmergencyBackup
    )

    data class EmergencyBackup(
        val file: File,
        val createdAtEpochMillis: Long,
        val totalRecords: Int
    )

    suspend fun exportToUri(uri: Uri): BackupSummary =
        withContext(Dispatchers.IO) {
            val document = createBackupDocument()
            val output = context.contentResolver.openOutputStream(uri)
                ?: error("Android could not open the selected backup file.")
            output.use { stream ->
                writeBackupZip(document, stream)
            }
            document.summary
        }

    suspend fun inspectUri(uri: Uri): BackupInspection =
        withContext(Dispatchers.IO) {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Android could not open the selected backup file.")
            val document = input.use(::readAndValidateBackupZip)
            BackupInspection(
                summary = document.summary,
                sourceLabel = queryDisplayName(uri) ?: "Selected backup"
            )
        }

    suspend fun restoreFromUri(uri: Uri): RestoreResult =
        withContext(Dispatchers.IO) {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Android could not open the selected backup file.")
            val document = input.use(::readAndValidateBackupZip)
            restoreValidatedDocument(document)
        }

    suspend fun restoreEmergencyBackup(file: File): RestoreResult =
        withContext(Dispatchers.IO) {
            require(file.exists()) {
                "The emergency backup is no longer available."
            }
            val document = FileInputStream(file).use(::readAndValidateBackupZip)
            restoreValidatedDocument(document)
        }

    suspend fun latestEmergencyBackup(): EmergencyBackup? =
        withContext(Dispatchers.IO) {
            emergencyDirectory()
                .listFiles { candidate ->
                    candidate.isFile && candidate.extension.equals("zip", true)
                }
                ?.maxByOrNull(File::lastModified)
                ?.let { file ->
                    runCatching {
                        FileInputStream(file).use(::readAndValidateBackupZip)
                    }.getOrNull()?.let { document ->
                        EmergencyBackup(
                            file = file,
                            createdAtEpochMillis = document.summary.createdAtEpochMillis,
                            totalRecords = document.summary.totalRecords
                        )
                    }
                }
        }

    fun suggestedFileName(now: Long = System.currentTimeMillis()): String {
        val date = Instant.ofEpochMilli(now)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm", Locale.US))
        return "DailyRebuild-Backup-$date.zip"
    }

    private suspend fun restoreValidatedDocument(
        document: BackupDocument
    ): RestoreResult {
        // A restore is never allowed to begin until the current database has
        // been preserved independently inside app-private storage.
        val emergency = createEmergencyBackup()

        database.withTransaction {
            val sqlite = database.openHelper.writableDatabase

            DELETE_ORDER.forEach { table ->
                sqlite.delete(table, null, null)
            }

            INSERT_ORDER.forEach { table ->
                val rows = document.tables.getJSONArray(table)
                for (index in 0 until rows.length()) {
                    val values = jsonRowToContentValues(
                        rows.getJSONObject(index)
                    )
                    val inserted = sqlite.insert(
                        table,
                        SQLiteDatabase.CONFLICT_ABORT,
                        values
                    )
                    check(inserted != -1L) {
                        "Could not restore a record in $table."
                    }
                }
            }

            sqlite.query("PRAGMA foreign_key_check").use { cursor ->
                check(!cursor.moveToFirst()) {
                    "The restored backup contains broken linked records."
                }
            }
        }

        return RestoreResult(
            summary = document.summary,
            emergencyBackup = emergency
        )
    }

    private fun createEmergencyBackup(): EmergencyBackup {
        val document = createBackupDocument()
        val directory = emergencyDirectory()
        val file = File(
            directory,
            "pre_restore_${document.summary.createdAtEpochMillis}.zip"
        )
        FileOutputStream(file).use { output ->
            writeBackupZip(document, output)
        }
        cleanupEmergencyBackups(directory)
        return EmergencyBackup(
            file = file,
            createdAtEpochMillis = document.summary.createdAtEpochMillis,
            totalRecords = document.summary.totalRecords
        )
    }

    private fun createBackupDocument(): BackupDocument {
        val sqlite = database.openHelper.writableDatabase
        val tablesObject = JSONObject()
        val tableCounts = linkedMapOf<String, Int>()

        INSERT_ORDER.forEach { table ->
            val rows = JSONArray()
            sqlite.query("SELECT * FROM `$table`").use { cursor ->
                while (cursor.moveToNext()) {
                    rows.put(cursorRowToJson(cursor))
                }
            }
            tablesObject.put(table, rows)
            tableCounts[table] = rows.length()
        }

        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            0
        )
        @Suppress("DEPRECATION")
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
        val versionName = packageInfo.versionName ?: "Unknown"
        val createdAt = System.currentTimeMillis()
        val totalRecords = tableCounts.values.sum()

        val tableCountsJson = JSONObject().apply {
            tableCounts.forEach { (table, count) ->
                put(table, count)
            }
        }

        val manifest = JSONObject().apply {
            put("formatName", FORMAT_NAME)
            put("formatVersion", FORMAT_VERSION)
            put("createdAtEpochMillis", createdAt)
            put("createdAtIso", Instant.ofEpochMilli(createdAt).toString())
            put("databaseVersion", DATABASE_VERSION)
            put("appPackage", context.packageName)
            put("appVersionName", versionName)
            put("appVersionCode", versionCode)
            put("totalRecords", totalRecords)
            put("tableCounts", tableCountsJson)
            put(
                "healthConnectNote",
                "Contains only Daily Rebuild-owned activity snapshots, not original Health Connect records."
            )
        }

        val data = JSONObject().apply {
            put("formatVersion", FORMAT_VERSION)
            put("tables", tablesObject)
        }

        return BackupDocument(
            manifest = manifest,
            data = data,
            summary = BackupSummary(
                createdAtEpochMillis = createdAt,
                appVersionName = versionName,
                databaseVersion = DATABASE_VERSION,
                totalRecords = totalRecords,
                tableCounts = tableCounts
            ),
            tables = tablesObject
        )
    }

    private fun writeBackupZip(
        document: BackupDocument,
        output: OutputStream
    ) {
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(document.manifest.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(document.data.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    private fun readAndValidateBackupZip(input: InputStream): BackupDocument {
        val entries = readZipEntries(input)
        val manifestBytes = entries[MANIFEST_ENTRY]
            ?: error("This ZIP does not contain $MANIFEST_ENTRY.")
        val dataBytes = entries[DATA_ENTRY]
            ?: error("This ZIP does not contain $DATA_ENTRY.")

        val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
        val data = JSONObject(dataBytes.toString(Charsets.UTF_8))

        require(manifest.optString("formatName") == FORMAT_NAME) {
            "This is not a Daily Rebuild backup."
        }
        require(manifest.optInt("formatVersion", -1) == FORMAT_VERSION) {
            "This backup format is not supported by this app version."
        }
        require(data.optInt("formatVersion", -1) == FORMAT_VERSION) {
            "The backup data format does not match its manifest."
        }

        val sourceDatabaseVersion = manifest.optInt("databaseVersion", -1)
        require(sourceDatabaseVersion == DATABASE_VERSION) {
            when {
                sourceDatabaseVersion > DATABASE_VERSION ->
                    "This backup was created by a newer Daily Rebuild database. Update the app before restoring it."
                sourceDatabaseVersion in 1 until DATABASE_VERSION ->
                    "This backup uses an older database format that this version cannot safely replace in full."
                else ->
                    "The backup does not contain a valid database version."
            }
        }

        val tables = data.optJSONObject("tables")
            ?: error("The backup does not contain its table data.")

        // Version 1 backups are complete snapshots. Refusing partial backups
        // prevents a malformed file from silently erasing an unrelated table.
        INSERT_ORDER.forEach { table ->
            require(tables.has(table) && tables.optJSONArray(table) != null) {
                "The backup is incomplete: $table is missing."
            }
            validateRowsForCurrentSchema(table, tables.getJSONArray(table))
        }

        val counts = linkedMapOf<String, Int>()
        INSERT_ORDER.forEach { table ->
            counts[table] = tables.getJSONArray(table).length()
        }
        val countedTotal = counts.values.sum()
        val manifestTotal = manifest.optInt("totalRecords", -1)
        require(manifestTotal == countedTotal) {
            "The backup record count does not match its data."
        }

        val summary = BackupSummary(
            createdAtEpochMillis = manifest.optLong("createdAtEpochMillis", 0L),
            appVersionName = manifest.optString("appVersionName", "Unknown"),
            databaseVersion = sourceDatabaseVersion,
            totalRecords = countedTotal,
            tableCounts = counts
        )
        require(summary.createdAtEpochMillis > 0L) {
            "The backup is missing its creation date."
        }

        return BackupDocument(
            manifest = manifest,
            data = data,
            summary = summary,
            tables = tables
        )
    }

    private fun validateRowsForCurrentSchema(
        table: String,
        rows: JSONArray
    ) {
        val columns = currentTableColumns(table)
        for (rowIndex in 0 until rows.length()) {
            val row = rows.optJSONObject(rowIndex)
                ?: error("$table contains a record that is not an object.")
            val keys = row.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                require(key in columns) {
                    "The backup contains an unsupported $table column: $key."
                }
            }
            columns.values.forEach { column ->
                require(row.has(column.name)) {
                    "A $table record is missing field ${column.name}."
                }
                if (column.requiredWithoutDefault) {
                    require(!row.isNull(column.name)) {
                        "A $table record has no value for required field ${column.name}."
                    }
                }
            }
        }
    }

    private fun currentTableColumns(table: String): Map<String, ColumnInfo> {
        val columns = linkedMapOf<String, ColumnInfo>()
        database.openHelper.writableDatabase
            .query("PRAGMA table_info(`$table`)")
            .use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                val notNullIndex = cursor.getColumnIndexOrThrow("notnull")
                val defaultIndex = cursor.getColumnIndexOrThrow("dflt_value")
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)
                    val notNull = cursor.getInt(notNullIndex) == 1
                    val hasDefault = !cursor.isNull(defaultIndex)
                    columns[name] = ColumnInfo(
                        name = name,
                        requiredWithoutDefault = notNull && !hasDefault
                    )
                }
            }
        require(columns.isNotEmpty()) {
            "The current app database does not contain $table."
        }
        return columns
    }

    private fun cursorRowToJson(cursor: Cursor): JSONObject {
        val row = JSONObject()
        for (index in 0 until cursor.columnCount) {
            val name = cursor.getColumnName(index)
            when (cursor.getType(index)) {
                Cursor.FIELD_TYPE_NULL -> row.put(name, JSONObject.NULL)
                Cursor.FIELD_TYPE_INTEGER -> row.put(name, cursor.getLong(index))
                Cursor.FIELD_TYPE_FLOAT -> row.put(name, cursor.getDouble(index))
                Cursor.FIELD_TYPE_STRING -> row.put(name, cursor.getString(index))
                Cursor.FIELD_TYPE_BLOB -> row.put(
                    name,
                    JSONObject().apply {
                        put(
                            BLOB_MARKER,
                            Base64.encodeToString(
                                cursor.getBlob(index),
                                Base64.NO_WRAP
                            )
                        )
                    }
                )
            }
        }
        return row
    }

    private fun jsonRowToContentValues(row: JSONObject): ContentValues {
        val values = ContentValues(row.length())
        val keys = row.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = row.get(key)
            when (value) {
                JSONObject.NULL -> values.putNull(key)
                is String -> values.put(key, value)
                is Int -> values.put(key, value)
                is Long -> values.put(key, value)
                is Double -> values.put(key, value)
                is Boolean -> values.put(key, if (value) 1L else 0L)
                is JSONObject -> {
                    require(value.has(BLOB_MARKER)) {
                        "Unsupported object value in backup field $key."
                    }
                    values.put(
                        key,
                        Base64.decode(
                            value.getString(BLOB_MARKER),
                            Base64.NO_WRAP
                        )
                    )
                }
                else -> error("Unsupported backup value in field $key.")
            }
        }
        return values
    }

    private fun readZipEntries(input: InputStream): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                require(entry.name in setOf(MANIFEST_ENTRY, DATA_ENTRY)) {
                    "The backup contains an unexpected file: ${entry.name}."
                }
                require(entry.name !in entries) {
                    "The backup contains duplicate ${entry.name} entries."
                }
                val bytes = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = zip.read(buffer)
                    if (read < 0) break
                    totalBytes += read
                    require(totalBytes <= MAX_UNCOMPRESSED_BYTES) {
                        "The backup is too large to restore safely."
                    }
                    bytes.write(buffer, 0, read)
                }
                entries[entry.name] = bytes.toByteArray()
                zip.closeEntry()
            }
        }
        return entries
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    private fun emergencyDirectory(): File =
        File(context.filesDir, "daily_rebuild_emergency_backups").apply {
            mkdirs()
        }

    private fun cleanupEmergencyBackups(directory: File) {
        directory.listFiles { file ->
            file.isFile && file.extension.equals("zip", true)
        }
            ?.sortedByDescending(File::lastModified)
            ?.drop(MAX_EMERGENCY_BACKUPS)
            ?.forEach { file -> file.delete() }
    }

    private data class BackupDocument(
        val manifest: JSONObject,
        val data: JSONObject,
        val summary: BackupSummary,
        val tables: JSONObject
    )

    private data class ColumnInfo(
        val name: String,
        val requiredWithoutDefault: Boolean
    )

    companion object {
        const val DATABASE_VERSION = 14
        const val FORMAT_VERSION = 1
        const val FORMAT_NAME = "Daily Rebuild Backup"

        private const val MANIFEST_ENTRY = "backup-manifest.json"
        private const val DATA_ENTRY = "daily-rebuild-backup.json"
        private const val BLOB_MARKER = "__daily_rebuild_blob_base64"
        private const val MAX_UNCOMPRESSED_BYTES = 100L * 1024L * 1024L
        private const val MAX_EMERGENCY_BACKUPS = 3

        /** Parent tables appear before children so linked records insert safely. */
        val INSERT_ORDER = listOf(
            "daily_records",
            "food_products",
            "saved_meals",
            "saved_meal_ingredients",
            "food_log_entries",
            "daily_activity_snapshots",
            "mobility_sessions",
            "health_profile",
            "health_measurements",
            "pain_activity_logs",
            "medication_entries",
            "calorie_goal_changes",
            "shower_logs",
            "migraine_logs",
            "saved_meetings",
            "meeting_attendance",
            "care_places",
            "care_providers",
            "care_visits",
            "care_appointments",
            "pantry_essentials"
        )

        val DELETE_ORDER = INSERT_ORDER.asReversed()
    }
}
