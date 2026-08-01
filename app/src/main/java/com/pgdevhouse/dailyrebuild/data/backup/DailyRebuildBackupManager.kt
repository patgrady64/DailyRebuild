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
import com.pgdevhouse.dailyrebuild.data.preferences.AppPreferencesRepository
import com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferences
import com.pgdevhouse.dailyrebuild.data.preferences.PortableAppPreferences
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
    private val appPreferencesRepository = AppPreferencesRepository(context)
    data class BackupSummary(
        val createdAtEpochMillis: Long,
        val appVersionName: String,
        val databaseVersion: Int,
        val formatVersion: Int,
        val totalRecords: Int,
        val tableCounts: Map<String, Int>,
        val preferencesIncluded: Boolean,
        val preferenceItemCount: Int
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
        val totalRecords: Int,
        val preferencesIncluded: Boolean
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
                            totalRecords = document.summary.totalRecords,
                            preferencesIncluded = document.summary.preferencesIncluded
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
        // A restore is never allowed to begin until both the current database
        // and current app setup have been preserved independently.
        val emergency = createEmergencyBackup()
        val preferencesBeforeRestore = appPreferencesRepository
            .exportPortablePreferences()

        try {
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

                // Version 1 backups contain database rows only. In that case,
                // deliberately retain the current installation's app setup.
                document.preferences?.let { portablePreferences ->
                    check(
                        appPreferencesRepository.restorePortablePreferences(
                            portablePreferences
                        )
                    ) {
                        "Android could not save the restored app preferences."
                    }
                }
            }
        } catch (error: Throwable) {
            // Preference writes are outside SQLite. Restore the previous setup
            // if any later database step fails so the operation remains whole.
            appPreferencesRepository.restorePortablePreferences(
                preferencesBeforeRestore
            )
            throw error
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
            totalRecords = document.summary.totalRecords,
            preferencesIncluded = document.summary.preferencesIncluded
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
        val portablePreferences = appPreferencesRepository
            .exportPortablePreferences()
        val preferencesJson = portablePreferencesToJson(portablePreferences)
        val preferenceItemCount = countPortablePreferenceItems(
            portablePreferences
        )

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
            put("preferencesIncluded", true)
            put("preferenceItemCount", preferenceItemCount)
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
            preferencesJson = preferencesJson,
            preferences = portablePreferences,
            summary = BackupSummary(
                createdAtEpochMillis = createdAt,
                appVersionName = versionName,
                databaseVersion = DATABASE_VERSION,
                formatVersion = FORMAT_VERSION,
                totalRecords = totalRecords,
                tableCounts = tableCounts,
                preferencesIncluded = true,
                preferenceItemCount = preferenceItemCount
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

            document.preferencesJson?.let { preferencesJson ->
                zip.putNextEntry(ZipEntry(USER_PREFERENCES_ENTRY))
                zip.write(preferencesJson.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    private fun readAndValidateBackupZip(input: InputStream): BackupDocument {
        val entries = readZipEntries(input)
        val manifestBytes = entries[MANIFEST_ENTRY]
            ?: error("This ZIP does not contain $MANIFEST_ENTRY.")
        val dataBytes = entries[DATA_ENTRY]
            ?: error("This ZIP does not contain $DATA_ENTRY.")

        val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
        val sourceData = JSONObject(dataBytes.toString(Charsets.UTF_8))

        require(manifest.optString("formatName") == FORMAT_NAME) {
            "This is not a Daily Rebuild backup."
        }

        val sourceFormatVersion = manifest.optInt("formatVersion", -1)
        require(sourceFormatVersion in MIN_SUPPORTED_FORMAT_VERSION..FORMAT_VERSION) {
            if (sourceFormatVersion > FORMAT_VERSION) {
                "This backup was created by a newer Daily Rebuild version. Update the app before restoring it."
            } else {
                "This backup format is not supported by this app version."
            }
        }
        require(sourceData.optInt("formatVersion", -1) == sourceFormatVersion) {
            "The backup data format does not match its manifest."
        }

        val preferencesBytes = entries[USER_PREFERENCES_ENTRY]
        val manifestSaysPreferencesIncluded = manifest.optBoolean(
            "preferencesIncluded",
            false
        )
        val preferencesJson: JSONObject?
        val portablePreferences: PortableAppPreferences?

        if (sourceFormatVersion >= PORTABLE_PREFERENCES_FORMAT_VERSION) {
            require(manifestSaysPreferencesIncluded) {
                "The backup manifest does not identify its app preferences."
            }
            val requiredPreferencesBytes = preferencesBytes
                ?: error("This backup is missing $USER_PREFERENCES_ENTRY.")
            preferencesJson = JSONObject(
                requiredPreferencesBytes.toString(Charsets.UTF_8)
            )
            require(
                preferencesJson.optInt("formatVersion", -1) ==
                    PORTABLE_PREFERENCES_FORMAT_VERSION
            ) {
                "The app preferences format is not supported."
            }
            portablePreferences = portablePreferencesFromJson(preferencesJson)
            val countedPreferenceItems = countPortablePreferenceItems(
                portablePreferences
            )
            require(
                manifest.optInt("preferenceItemCount", -1) ==
                    countedPreferenceItems
            ) {
                "The backup preference count does not match its data."
            }
        } else {
            // Format 1 backups predate portable preferences. They remain valid,
            // and restoring them deliberately keeps the current app setup.
            require(preferencesBytes == null) {
                "This older backup contains an unexpected preferences file."
            }
            preferencesJson = null
            portablePreferences = null
        }

        val sourceDatabaseVersion = manifest.optInt("databaseVersion", -1)
        require(sourceDatabaseVersion in MIN_SUPPORTED_DATABASE_VERSION..DATABASE_VERSION) {
            when {
                sourceDatabaseVersion > DATABASE_VERSION ->
                    "This backup was created by a newer Daily Rebuild database. Update the app before restoring it."
                sourceDatabaseVersion in 1 until MIN_SUPPORTED_DATABASE_VERSION ->
                    "This backup is too old for this version to restore safely."
                else ->
                    "The backup does not contain a valid database version."
            }
        }

        val sourceTables = sourceData.optJSONObject("tables")
            ?: error("The backup does not contain its table data.")

        // Verify the snapshot as it was originally written before adding any
        // compatibility defaults required by newer database versions.
        val sourceTableNames = sourceTables.keys().asSequence().toList()
        sourceTableNames.forEach { table ->
            require(table in INSERT_ORDER) {
                "The backup contains an unsupported table: $table."
            }
            require(sourceTables.optJSONArray(table) != null) {
                "The backup table $table is not a record list."
            }
        }
        val sourceRecordCount = sourceTableNames.sumOf { table ->
            sourceTables.getJSONArray(table).length()
        }
        require(manifest.optInt("totalRecords", -1) == sourceRecordCount) {
            "The backup record count does not match its data."
        }

        val tables = normalizeTablesForCurrentSchema(
            sourceTables = sourceTables,
            sourceDatabaseVersion = sourceDatabaseVersion
        )

        // A restored snapshot must now be complete for the current schema.
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
        val preferenceItemCount = portablePreferences
            ?.let(::countPortablePreferenceItems)
            ?: 0

        val summary = BackupSummary(
            createdAtEpochMillis = manifest.optLong("createdAtEpochMillis", 0L),
            appVersionName = manifest.optString("appVersionName", "Unknown"),
            databaseVersion = sourceDatabaseVersion,
            formatVersion = sourceFormatVersion,
            totalRecords = countedTotal,
            tableCounts = counts,
            preferencesIncluded = portablePreferences != null,
            preferenceItemCount = preferenceItemCount
        )
        require(summary.createdAtEpochMillis > 0L) {
            "The backup is missing its creation date."
        }

        val normalizedData = JSONObject().apply {
            put("formatVersion", FORMAT_VERSION)
            put("tables", tables)
        }

        return BackupDocument(
            manifest = manifest,
            data = normalizedData,
            preferencesJson = preferencesJson,
            preferences = portablePreferences,
            summary = summary,
            tables = tables
        )
    }

    private fun normalizeTablesForCurrentSchema(
        sourceTables: JSONObject,
        sourceDatabaseVersion: Int
    ): JSONObject {
        val normalized = JSONObject()

        INSERT_ORDER.forEach { table ->
            val rows = sourceTables.optJSONArray(table)
            val compatibleRows = when {
                rows != null -> rows
                table == "life_maintenance_logs" &&
                    sourceDatabaseVersion < LIFE_MAINTENANCE_DATABASE_VERSION -> JSONArray()
                table == "iop_groups" &&
                    sourceDatabaseVersion < IOP_GROUP_DATABASE_VERSION -> defaultIopGroupRows()
                table == "iop_missed_occurrences" &&
                    sourceDatabaseVersion < IOP_ATTENDANCE_DATABASE_VERSION -> JSONArray()
                else -> error("The backup is incomplete: $table is missing.")
            }

            if (table == "food_log_entries") {
                for (index in 0 until compatibleRows.length()) {
                    val row = compatibleRows.optJSONObject(index)
                        ?: error("$table contains a record that is not an object.")
                    if (!row.has("savedMealId")) {
                        row.put("savedMealId", JSONObject.NULL)
                    }
                    if (!row.has("mealQuantity")) {
                        row.put("mealQuantity", 1.0)
                    }
                }
            }

            normalized.put(table, compatibleRows)
        }

        return normalized
    }

    private fun defaultIopGroupRows(): JSONArray {
        val timestamp = System.currentTimeMillis()
        return JSONArray().apply {
            listOf(
                1 to "Monday",
                2 to "Tuesday",
                3 to "Wednesday",
                4 to "Thursday"
            ).forEachIndexed { index, (dayOfWeek, dayLabel) ->
                put(
                    JSONObject().apply {
                        put("id", index + 1L)
                        put("name", "$dayLabel IOP Group")
                        put("dayOfWeek", dayOfWeek)
                        put("startMinutes", 18 * 60 + 30)
                        put("endMinutes", 20 * 60 + 30)
                        put("location", "")
                        put("notes", "")
                        put("active", 1)
                        put("createdAt", timestamp)
                        put("updatedAt", timestamp)
                    }
                )
            }
        }
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

    private fun portablePreferencesToJson(
        value: PortableAppPreferences
    ): JSONObject {
        val settings = value.settings
        return JSONObject().apply {
            put("formatVersion", PORTABLE_PREFERENCES_FORMAT_VERSION)
            put(
                "settings",
                JSONObject().apply {
                    put(
                        "enabledLogSections",
                        orderedSetJson(
                            settings.enabledLogSections,
                            DailyRebuildPreferences.defaultLogSections.toList()
                        )
                    )
                    put("quickLogOrder", stringListJson(settings.quickLogOrder))
                    put(
                        "hiddenQuickLogActions",
                        orderedSetJson(
                            settings.hiddenQuickLogActions,
                            DailyRebuildPreferences.defaultQuickLogOrder
                        )
                    )
                    put(
                        "visibleTodaySections",
                        orderedSetJson(
                            settings.visibleTodaySections,
                            DailyRebuildPreferences.defaultTodaySections.toList()
                        )
                    )
                    put("statsOrder", stringListJson(settings.statsOrder))
                    put(
                        "hiddenStatsSections",
                        orderedSetJson(
                            settings.hiddenStatsSections,
                            DailyRebuildPreferences.defaultStatsOrder
                        )
                    )
                    put("statsDefaultRange", settings.statsDefaultRange)
                    put(
                        "appointmentRemindersEnabled",
                        settings.appointmentRemindersEnabled
                    )
                    put(
                        "meetingRemindersEnabled",
                        settings.meetingRemindersEnabled
                    )
                    put("iopRemindersEnabled", settings.iopRemindersEnabled)
                    put("weightUnit", settings.weightUnit)
                    put("distanceUnit", settings.distanceUnit)
                    put("waterUnit", settings.waterUnit)
                    put("temperatureUnit", settings.temperatureUnit)
                    put("foodMassUnit", settings.foodMassUnit)
                    put("heightUnit", settings.heightUnit)
                }
            )
            put("recentSearches", stringListJson(value.recentSearches))
            put(
                "ignoredDataQualitySignatures",
                stringListJson(value.ignoredDataQualitySignatures.sorted())
            )
            put("iopDefaultsInitialized", value.iopDefaultsInitialized)
        }
    }

    private fun portablePreferencesFromJson(
        root: JSONObject
    ): PortableAppPreferences {
        val settingsJson = root.optJSONObject("settings")
            ?: error("The backup app preferences do not contain settings.")

        val enabledLogSections = requiredStringSet(
            settingsJson,
            "enabledLogSections",
            DailyRebuildPreferences.defaultLogSections.size,
            MAX_PREFERENCE_ID_LENGTH
        )
        require(enabledLogSections.isNotEmpty()) {
            "The backup must keep at least one Log section enabled."
        }
        require(
            enabledLogSections.all {
                it in DailyRebuildPreferences.defaultLogSections
            }
        ) {
            "The backup contains an unsupported Log section preference."
        }

        val quickLogOrder = requiredStringList(
            settingsJson,
            "quickLogOrder",
            DailyRebuildPreferences.defaultQuickLogOrder.size,
            MAX_PREFERENCE_ID_LENGTH
        )
        require(
            quickLogOrder.size == DailyRebuildPreferences.defaultQuickLogOrder.size &&
                quickLogOrder.toSet() ==
                DailyRebuildPreferences.defaultQuickLogOrder.toSet()
        ) {
            "The backup Quick Log order is incomplete or invalid."
        }

        val hiddenQuickLogActions = requiredStringSet(
            settingsJson,
            "hiddenQuickLogActions",
            DailyRebuildPreferences.defaultQuickLogOrder.size,
            MAX_PREFERENCE_ID_LENGTH
        )
        require(
            hiddenQuickLogActions.all {
                it in DailyRebuildPreferences.defaultQuickLogOrder
            }
        ) {
            "The backup contains an unsupported hidden Quick Log action."
        }

        val visibleTodaySections = requiredStringSet(
            settingsJson,
            "visibleTodaySections",
            DailyRebuildPreferences.defaultTodaySections.size,
            MAX_PREFERENCE_ID_LENGTH
        )
        require(
            visibleTodaySections.all {
                it in DailyRebuildPreferences.defaultTodaySections
            }
        ) {
            "The backup contains an unsupported Today section preference."
        }

        val statsOrder = requiredStringList(
            settingsJson,
            "statsOrder",
            DailyRebuildPreferences.defaultStatsOrder.size,
            MAX_PREFERENCE_ID_LENGTH
        )
        require(
            statsOrder.size == DailyRebuildPreferences.defaultStatsOrder.size &&
                statsOrder.toSet() == DailyRebuildPreferences.defaultStatsOrder.toSet()
        ) {
            "The backup Stats order is incomplete or invalid."
        }

        val hiddenStatsSections = requiredStringSet(
            settingsJson,
            "hiddenStatsSections",
            DailyRebuildPreferences.defaultStatsOrder.size,
            MAX_PREFERENCE_ID_LENGTH
        )
        require(
            hiddenStatsSections.all {
                it in DailyRebuildPreferences.defaultStatsOrder
            }
        ) {
            "The backup contains an unsupported hidden Stats section."
        }

        val settings = DailyRebuildPreferences(
            enabledLogSections = enabledLogSections,
            quickLogOrder = quickLogOrder,
            hiddenQuickLogActions = hiddenQuickLogActions,
            visibleTodaySections = visibleTodaySections,
            statsOrder = statsOrder,
            hiddenStatsSections = hiddenStatsSections,
            statsDefaultRange = requiredAllowedString(
                settingsJson,
                "statsDefaultRange",
                VALID_STATS_RANGES
            ),
            appointmentRemindersEnabled = requiredBoolean(
                settingsJson,
                "appointmentRemindersEnabled"
            ),
            meetingRemindersEnabled = requiredBoolean(
                settingsJson,
                "meetingRemindersEnabled"
            ),
            iopRemindersEnabled = requiredBoolean(
                settingsJson,
                "iopRemindersEnabled"
            ),
            weightUnit = requiredAllowedString(
                settingsJson,
                "weightUnit",
                VALID_WEIGHT_UNITS
            ),
            distanceUnit = requiredAllowedString(
                settingsJson,
                "distanceUnit",
                VALID_DISTANCE_UNITS
            ),
            waterUnit = requiredAllowedString(
                settingsJson,
                "waterUnit",
                VALID_WATER_UNITS
            ),
            temperatureUnit = requiredAllowedString(
                settingsJson,
                "temperatureUnit",
                VALID_TEMPERATURE_UNITS
            ),
            foodMassUnit = requiredAllowedString(
                settingsJson,
                "foodMassUnit",
                VALID_FOOD_MASS_UNITS
            ),
            heightUnit = requiredAllowedString(
                settingsJson,
                "heightUnit",
                VALID_HEIGHT_UNITS
            )
        )

        val recentSearches = requiredStringList(
            root,
            "recentSearches",
            MAX_RECENT_SEARCHES,
            MAX_RECENT_SEARCH_LENGTH
        )
        require(
            recentSearches.map { it.lowercase() }.distinct().size ==
                recentSearches.size
        ) {
            "The backup contains duplicate recent searches."
        }

        val ignoredSignatures = requiredStringSet(
            root,
            "ignoredDataQualitySignatures",
            MAX_IGNORED_SIGNATURES,
            MAX_IGNORED_SIGNATURE_LENGTH
        )

        return PortableAppPreferences(
            settings = settings,
            recentSearches = recentSearches,
            ignoredDataQualitySignatures = ignoredSignatures,
            iopDefaultsInitialized = requiredBoolean(
                root,
                "iopDefaultsInitialized"
            )
        )
    }

    private fun countPortablePreferenceItems(
        value: PortableAppPreferences
    ): Int = PORTABLE_SETTINGS_FIELD_COUNT +
        value.recentSearches.size +
        value.ignoredDataQualitySignatures.size +
        1

    private fun orderedSetJson(
        values: Set<String>,
        order: List<String>
    ): JSONArray = stringListJson(order.filter(values::contains))

    private fun stringListJson(values: List<String>): JSONArray =
        JSONArray().apply {
            values.forEach(::put)
        }

    private fun requiredStringList(
        objectValue: JSONObject,
        key: String,
        maximumCount: Int,
        maximumLength: Int
    ): List<String> {
        val array = objectValue.optJSONArray(key)
            ?: error("The backup app preferences are missing $key.")
        require(array.length() <= maximumCount) {
            "The backup app preference $key contains too many values."
        }

        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val value = array.optString(index, null)
                    ?: error("The backup app preference $key contains a non-text value.")
                require(value.isNotBlank() && value.length <= maximumLength) {
                    "The backup app preference $key contains an invalid value."
                }
                add(value)
            }
        }
    }

    private fun requiredStringSet(
        objectValue: JSONObject,
        key: String,
        maximumCount: Int,
        maximumLength: Int
    ): Set<String> {
        val values = requiredStringList(
            objectValue,
            key,
            maximumCount,
            maximumLength
        )
        require(values.distinct().size == values.size) {
            "The backup app preference $key contains duplicate values."
        }
        return values.toCollection(linkedSetOf())
    }

    private fun requiredAllowedString(
        objectValue: JSONObject,
        key: String,
        allowed: Set<String>
    ): String {
        require(objectValue.has(key) && !objectValue.isNull(key)) {
            "The backup app preferences are missing $key."
        }
        val value = objectValue.optString(key, "")
        require(value in allowed) {
            "The backup app preference $key is not supported."
        }
        return value
    }

    private fun requiredBoolean(
        objectValue: JSONObject,
        key: String
    ): Boolean {
        require(objectValue.has(key) && !objectValue.isNull(key)) {
            "The backup app preferences are missing $key."
        }
        val value = objectValue.get(key)
        require(value is Boolean) {
            "The backup app preference $key is not true or false."
        }
        return value
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
                require(
                    entry.name in setOf(
                        MANIFEST_ENTRY,
                        DATA_ENTRY,
                        USER_PREFERENCES_ENTRY
                    )
                ) {
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
        val preferencesJson: JSONObject?,
        val preferences: PortableAppPreferences?,
        val summary: BackupSummary,
        val tables: JSONObject
    )

    private data class ColumnInfo(
        val name: String,
        val requiredWithoutDefault: Boolean
    )

    companion object {
        const val DATABASE_VERSION = 18
        private const val MIN_SUPPORTED_DATABASE_VERSION = 14
        private const val LIFE_MAINTENANCE_DATABASE_VERSION = 15
        private const val IOP_GROUP_DATABASE_VERSION = 17
        private const val IOP_ATTENDANCE_DATABASE_VERSION = 18
        const val FORMAT_VERSION = 2
        private const val MIN_SUPPORTED_FORMAT_VERSION = 1
        private const val PORTABLE_PREFERENCES_FORMAT_VERSION = 2
        const val FORMAT_NAME = "Daily Rebuild Backup"

        private const val MANIFEST_ENTRY = "backup-manifest.json"
        private const val DATA_ENTRY = "daily-rebuild-backup.json"
        private const val USER_PREFERENCES_ENTRY = "user-preferences.json"
        private const val BLOB_MARKER = "__daily_rebuild_blob_base64"
        private const val MAX_UNCOMPRESSED_BYTES = 100L * 1024L * 1024L
        private const val MAX_EMERGENCY_BACKUPS = 3
        private const val PORTABLE_SETTINGS_FIELD_COUNT = 16
        private const val MAX_PREFERENCE_ID_LENGTH = 64
        private const val MAX_RECENT_SEARCHES = 8
        private const val MAX_RECENT_SEARCH_LENGTH = 200
        private const val MAX_IGNORED_SIGNATURES = 500
        private const val MAX_IGNORED_SIGNATURE_LENGTH = 500

        private val VALID_STATS_RANGES = setOf(
            "LAST_7_DAYS",
            "LAST_30_DAYS",
            "LAST_90_DAYS",
            "CUSTOM",
            "ALL_TIME"
        )
        private val VALID_WEIGHT_UNITS = setOf("lb", "kg")
        private val VALID_DISTANCE_UNITS = setOf("mi", "km")
        private val VALID_WATER_UNITS = setOf("oz", "ml")
        private val VALID_TEMPERATURE_UNITS = setOf("f", "c")
        private val VALID_FOOD_MASS_UNITS = setOf("oz", "g")
        private val VALID_HEIGHT_UNITS = setOf("ft_in", "cm")

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
            "pantry_essentials",
            "life_maintenance_logs",
            "iop_groups",
            "iop_missed_occurrences"
        )

        val DELETE_ORDER = INSERT_ORDER.asReversed()
    }
}
