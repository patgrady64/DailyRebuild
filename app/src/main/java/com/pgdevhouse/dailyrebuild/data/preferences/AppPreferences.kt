package com.pgdevhouse.dailyrebuild.data.preferences

import android.content.Context
import android.content.SharedPreferences

object DailyRebuildPreferenceIds {
    const val LOG_FOOD = "food"
    const val LOG_MOVEMENT = "movement"
    const val LOG_MEETINGS = "meetings"
    const val LOG_HEALTH = "health"
    const val LOG_MAINTENANCE = "maintenance"

    const val QUICK_FOOD = "food"
    const val QUICK_WATER = "water"
    const val QUICK_MOBILITY = "mobility"
    const val QUICK_PAIN = "pain"
    const val QUICK_SHOWER = "shower"
    const val QUICK_MAINTENANCE = "maintenance"
    const val QUICK_MEETINGS = "meetings"
    const val QUICK_HEALTH = "health"

    const val TODAY_PRIORITY = "priority"
    const val TODAY_GLANCE = "glance"
    const val TODAY_APPOINTMENTS = "appointments"
    const val TODAY_IOP = "iop"
    const val TODAY_MEETINGS = "meetings"
    const val TODAY_MOVEMENT = "movement"
    const val TODAY_QUICK_LOG = "quick_log"
    const val TODAY_RECENT = "recent_frequent"
    const val TODAY_ACTIVITY = "today_activity"
    const val TODAY_SAVE_STATUS = "save_status"
    const val TODAY_MORE = "more"

    const val STATS_NUTRITION = "nutrition"
    const val STATS_WATER = "water"
    const val STATS_PAIN = "pain"
    const val STATS_MOBILITY = "mobility"
    const val STATS_HEALTH = "health"
    const val STATS_MIGRAINE = "migraine"
    const val STATS_MAINTENANCE = "maintenance"
    const val STATS_MEETINGS = "meetings"
}

data class DailyRebuildPreferences(
    val enabledLogSections: Set<String> = defaultLogSections,
    val quickLogOrder: List<String> = defaultQuickLogOrder,
    val hiddenQuickLogActions: Set<String> = emptySet(),
    val visibleTodaySections: Set<String> = defaultTodaySections,
    val statsOrder: List<String> = defaultStatsOrder,
    val hiddenStatsSections: Set<String> = emptySet(),
    val statsDefaultRange: String = "LAST_30_DAYS",
    val notificationsEnabled: Boolean = true,
    val appointmentRemindersEnabled: Boolean = true,
    val meetingRemindersEnabled: Boolean = false,
    val iopRemindersEnabled: Boolean = false,
    val iopAttendanceFollowUpEnabled: Boolean = false,
    val meetingReminderLeadMinutes: Int = 60,
    val iopReminderLeadMinutes: Int = 60,
    val notificationSnoozeMinutes: Int = 15,
    val weightUnit: String = "lb",
    val distanceUnit: String = "mi",
    val waterUnit: String = "oz",
    val temperatureUnit: String = "f",
    val foodMassUnit: String = "oz",
    val heightUnit: String = "ft_in"
) {
    companion object {
        val defaultLogSections = linkedSetOf(
            DailyRebuildPreferenceIds.LOG_FOOD,
            DailyRebuildPreferenceIds.LOG_MOVEMENT,
            DailyRebuildPreferenceIds.LOG_MEETINGS,
            DailyRebuildPreferenceIds.LOG_HEALTH,
            DailyRebuildPreferenceIds.LOG_MAINTENANCE
        )

        val defaultQuickLogOrder = listOf(
            DailyRebuildPreferenceIds.QUICK_FOOD,
            DailyRebuildPreferenceIds.QUICK_WATER,
            DailyRebuildPreferenceIds.QUICK_MOBILITY,
            DailyRebuildPreferenceIds.QUICK_PAIN,
            DailyRebuildPreferenceIds.QUICK_SHOWER,
            DailyRebuildPreferenceIds.QUICK_MAINTENANCE,
            DailyRebuildPreferenceIds.QUICK_MEETINGS,
            DailyRebuildPreferenceIds.QUICK_HEALTH
        )

        val defaultTodaySections = linkedSetOf(
            DailyRebuildPreferenceIds.TODAY_PRIORITY,
            DailyRebuildPreferenceIds.TODAY_GLANCE,
            DailyRebuildPreferenceIds.TODAY_APPOINTMENTS,
            DailyRebuildPreferenceIds.TODAY_IOP,
            DailyRebuildPreferenceIds.TODAY_MEETINGS,
            DailyRebuildPreferenceIds.TODAY_MOVEMENT,
            DailyRebuildPreferenceIds.TODAY_QUICK_LOG,
            DailyRebuildPreferenceIds.TODAY_RECENT,
            DailyRebuildPreferenceIds.TODAY_ACTIVITY,
            DailyRebuildPreferenceIds.TODAY_SAVE_STATUS,
            DailyRebuildPreferenceIds.TODAY_MORE
        )

        val defaultStatsOrder = listOf(
            DailyRebuildPreferenceIds.STATS_NUTRITION,
            DailyRebuildPreferenceIds.STATS_WATER,
            DailyRebuildPreferenceIds.STATS_PAIN,
            DailyRebuildPreferenceIds.STATS_MOBILITY,
            DailyRebuildPreferenceIds.STATS_MEETINGS,
            DailyRebuildPreferenceIds.STATS_HEALTH,
            DailyRebuildPreferenceIds.STATS_MIGRAINE,
            DailyRebuildPreferenceIds.STATS_MAINTENANCE
        )
    }
}

/**
 * The non-database state that travels with a portable Daily Rebuild backup.
 * Keeping this model separate from SharedPreferences makes validation and
 * backward-compatible restores explicit instead of copying an opaque XML file.
 */
data class PortableAppPreferences(
    val settings: DailyRebuildPreferences,
    val recentSearches: List<String>,
    val ignoredDataQualitySignatures: Set<String>,
    val iopDefaultsInitialized: Boolean
)

class AppPreferencesRepository(
    context: Context
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        FILE_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): DailyRebuildPreferences {
        val enabledLogSections = readSet(
            KEY_ENABLED_LOG_SECTIONS,
            DailyRebuildPreferences.defaultLogSections
        ).intersect(DailyRebuildPreferences.defaultLogSections)
            .ifEmpty { setOf(DailyRebuildPreferenceIds.LOG_FOOD) }

        val storedTodaySections = readSet(
            KEY_VISIBLE_TODAY_SECTIONS,
            DailyRebuildPreferences.defaultTodaySections
        ).intersect(DailyRebuildPreferences.defaultTodaySections)

        val visibleTodaySections =
            if (!preferences.getBoolean(KEY_TODAY_PHASE2_INITIALIZED, false)) {
                val upgraded = storedTodaySections + setOf(
                    DailyRebuildPreferenceIds.TODAY_RECENT,
                    DailyRebuildPreferenceIds.TODAY_ACTIVITY
                )
                preferences.edit()
                    .putStringSet(KEY_VISIBLE_TODAY_SECTIONS, upgraded)
                    .putBoolean(KEY_TODAY_PHASE2_INITIALIZED, true)
                    .apply()
                upgraded
            } else {
                storedTodaySections
            }

        return DailyRebuildPreferences(
            enabledLogSections = enabledLogSections,
            quickLogOrder = readOrderedList(
                KEY_QUICK_LOG_ORDER,
                DailyRebuildPreferences.defaultQuickLogOrder
            ),
            hiddenQuickLogActions = readSet(
                KEY_HIDDEN_QUICK_LOG_ACTIONS,
                emptySet()
            ).intersect(DailyRebuildPreferences.defaultQuickLogOrder.toSet()),
            visibleTodaySections = visibleTodaySections,
            statsOrder = readOrderedList(
                KEY_STATS_ORDER,
                DailyRebuildPreferences.defaultStatsOrder
            ),
            hiddenStatsSections = readSet(
                KEY_HIDDEN_STATS_SECTIONS,
                emptySet()
            ).intersect(DailyRebuildPreferences.defaultStatsOrder.toSet()),
            statsDefaultRange = preferences.getString(
                KEY_STATS_DEFAULT_RANGE,
                "LAST_30_DAYS"
            )?.takeIf { it in VALID_STATS_RANGES } ?: "LAST_30_DAYS",
            notificationsEnabled = preferences.getBoolean(
                KEY_NOTIFICATIONS_ENABLED,
                true
            ),
            appointmentRemindersEnabled = preferences.getBoolean(
                KEY_APPOINTMENT_REMINDERS,
                true
            ),
            meetingRemindersEnabled = preferences.getBoolean(
                KEY_MEETING_REMINDERS,
                false
            ),
            iopRemindersEnabled = preferences.getBoolean(
                KEY_IOP_REMINDERS,
                false
            ),
            iopAttendanceFollowUpEnabled = preferences.getBoolean(
                KEY_IOP_ATTENDANCE_FOLLOW_UP,
                false
            ),
            meetingReminderLeadMinutes = preferences.getInt(
                KEY_MEETING_REMINDER_LEAD_MINUTES,
                60
            ).takeIf { it in VALID_REMINDER_LEAD_MINUTES } ?: 60,
            iopReminderLeadMinutes = preferences.getInt(
                KEY_IOP_REMINDER_LEAD_MINUTES,
                60
            ).takeIf { it in VALID_REMINDER_LEAD_MINUTES } ?: 60,
            notificationSnoozeMinutes = preferences.getInt(
                KEY_NOTIFICATION_SNOOZE_MINUTES,
                15
            ).takeIf { it in VALID_SNOOZE_MINUTES } ?: 15,
            weightUnit = preferences.getString(KEY_WEIGHT_UNIT, "lb")
                ?.takeIf { it in VALID_WEIGHT_UNITS } ?: "lb",
            distanceUnit = preferences.getString(KEY_DISTANCE_UNIT, "mi")
                ?.takeIf { it in VALID_DISTANCE_UNITS } ?: "mi",
            waterUnit = preferences.getString(KEY_WATER_UNIT, "oz")
                ?.takeIf { it in VALID_WATER_UNITS } ?: "oz",
            temperatureUnit = preferences.getString(KEY_TEMPERATURE_UNIT, "f")
                ?.takeIf { it in VALID_TEMPERATURE_UNITS } ?: "f",
            foodMassUnit = preferences.getString(KEY_FOOD_MASS_UNIT, "oz")
                ?.takeIf { it in VALID_FOOD_MASS_UNITS } ?: "oz",
            heightUnit = preferences.getString(KEY_HEIGHT_UNIT, "ft_in")
                ?.takeIf { it in VALID_HEIGHT_UNITS } ?: "ft_in"
        )
    }

    fun save(value: DailyRebuildPreferences) {
        writeSettings(preferences.edit(), normalizeSettings(value))
            .putBoolean(KEY_TODAY_PHASE2_INITIALIZED, true)
            .apply()
    }

    fun exportPortablePreferences(): PortableAppPreferences =
        PortableAppPreferences(
            settings = load(),
            recentSearches = loadRecentSearches(),
            ignoredDataQualitySignatures = loadIgnoredDataQualitySignatures(),
            iopDefaultsInitialized = areIopDefaultsInitialized()
        )

    /**
     * Restores all known portable settings synchronously. A synchronous commit
     * lets the backup manager keep the database and preference restore together.
     */
    fun restorePortablePreferences(value: PortableAppPreferences): Boolean {
        val normalized = PortableAppPreferences(
            settings = normalizeSettings(value.settings),
            recentSearches = normalizeRecentSearches(value.recentSearches),
            ignoredDataQualitySignatures = normalizeIgnoredSignatures(
                value.ignoredDataQualitySignatures
            ),
            iopDefaultsInitialized = value.iopDefaultsInitialized
        )

        val editor = writeSettings(
            preferences.edit(),
            normalized.settings
        )
            .putBoolean(KEY_IOP_DEFAULTS_INITIALIZED, normalized.iopDefaultsInitialized)
            // The restored visible-section set is already the user's deliberate
            // choice, so do not run the old Phase 2 auto-enable migration again.
            .putBoolean(KEY_TODAY_PHASE2_INITIALIZED, true)

        if (normalized.recentSearches.isEmpty()) {
            editor.remove(KEY_RECENT_SEARCHES)
        } else {
            editor.putString(
                KEY_RECENT_SEARCHES,
                normalized.recentSearches.joinToString(RECENT_SEARCH_SEPARATOR)
            )
        }

        if (normalized.ignoredDataQualitySignatures.isEmpty()) {
            editor.remove(KEY_IGNORED_DATA_QUALITY_SIGNATURES)
        } else {
            editor.putStringSet(
                KEY_IGNORED_DATA_QUALITY_SIGNATURES,
                normalized.ignoredDataQualitySignatures
            )
        }

        return editor.commit()
    }

    fun loadRecentSearches(): List<String> {
        return normalizeRecentSearches(
            preferences.getString(KEY_RECENT_SEARCHES, null)
                ?.split(RECENT_SEARCH_SEPARATOR)
                .orEmpty()
        )
    }

    fun rememberSearch(query: String): List<String> {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return loadRecentSearches()

        val updated = normalizeRecentSearches(
            listOf(cleaned) + loadRecentSearches()
                .filterNot { it.equals(cleaned, ignoreCase = true) }
        )

        preferences.edit()
            .putString(KEY_RECENT_SEARCHES, updated.joinToString(RECENT_SEARCH_SEPARATOR))
            .apply()

        return updated
    }

    fun clearRecentSearches() {
        preferences.edit()
            .remove(KEY_RECENT_SEARCHES)
            .apply()
    }

    fun loadIgnoredDataQualitySignatures(): Set<String> =
        normalizeIgnoredSignatures(
            preferences.getStringSet(KEY_IGNORED_DATA_QUALITY_SIGNATURES, emptySet())
                ?.toSet()
                .orEmpty()
        )

    fun ignoreDataQualitySignature(signature: String) {
        if (signature.isBlank()) return
        val updated = normalizeIgnoredSignatures(
            loadIgnoredDataQualitySignatures() + signature
        )
        preferences.edit()
            .putStringSet(KEY_IGNORED_DATA_QUALITY_SIGNATURES, updated)
            .apply()
    }

    fun clearIgnoredDataQualitySignatures() {
        preferences.edit()
            .remove(KEY_IGNORED_DATA_QUALITY_SIGNATURES)
            .apply()
    }

    fun areIopDefaultsInitialized(): Boolean =
        preferences.getBoolean(KEY_IOP_DEFAULTS_INITIALIZED, false)

    fun markIopDefaultsInitialized() {
        preferences.edit()
            .putBoolean(KEY_IOP_DEFAULTS_INITIALIZED, true)
            .apply()
    }

    private fun writeSettings(
        editor: SharedPreferences.Editor,
        value: DailyRebuildPreferences
    ): SharedPreferences.Editor = editor
        .putStringSet(KEY_ENABLED_LOG_SECTIONS, value.enabledLogSections)
        .putString(KEY_QUICK_LOG_ORDER, value.quickLogOrder.joinToString(SEPARATOR))
        .putStringSet(KEY_HIDDEN_QUICK_LOG_ACTIONS, value.hiddenQuickLogActions)
        .putStringSet(KEY_VISIBLE_TODAY_SECTIONS, value.visibleTodaySections)
        .putString(KEY_STATS_ORDER, value.statsOrder.joinToString(SEPARATOR))
        .putStringSet(KEY_HIDDEN_STATS_SECTIONS, value.hiddenStatsSections)
        .putString(KEY_STATS_DEFAULT_RANGE, value.statsDefaultRange)
        .putBoolean(KEY_NOTIFICATIONS_ENABLED, value.notificationsEnabled)
        .putBoolean(KEY_APPOINTMENT_REMINDERS, value.appointmentRemindersEnabled)
        .putBoolean(KEY_MEETING_REMINDERS, value.meetingRemindersEnabled)
        .putBoolean(KEY_IOP_REMINDERS, value.iopRemindersEnabled)
        .putBoolean(KEY_IOP_ATTENDANCE_FOLLOW_UP, value.iopAttendanceFollowUpEnabled)
        .putInt(KEY_MEETING_REMINDER_LEAD_MINUTES, value.meetingReminderLeadMinutes)
        .putInt(KEY_IOP_REMINDER_LEAD_MINUTES, value.iopReminderLeadMinutes)
        .putInt(KEY_NOTIFICATION_SNOOZE_MINUTES, value.notificationSnoozeMinutes)
        .putString(KEY_WEIGHT_UNIT, value.weightUnit)
        .putString(KEY_DISTANCE_UNIT, value.distanceUnit)
        .putString(KEY_WATER_UNIT, value.waterUnit)
        .putString(KEY_TEMPERATURE_UNIT, value.temperatureUnit)
        .putString(KEY_FOOD_MASS_UNIT, value.foodMassUnit)
        .putString(KEY_HEIGHT_UNIT, value.heightUnit)

    private fun normalizeSettings(value: DailyRebuildPreferences): DailyRebuildPreferences {
        val enabledLogSections = value.enabledLogSections
            .intersect(DailyRebuildPreferences.defaultLogSections)
            .ifEmpty { setOf(DailyRebuildPreferenceIds.LOG_FOOD) }

        return value.copy(
            enabledLogSections = enabledLogSections,
            quickLogOrder = normalizeOrderedList(
                value.quickLogOrder,
                DailyRebuildPreferences.defaultQuickLogOrder
            ),
            hiddenQuickLogActions = value.hiddenQuickLogActions
                .intersect(DailyRebuildPreferences.defaultQuickLogOrder.toSet()),
            visibleTodaySections = value.visibleTodaySections
                .intersect(DailyRebuildPreferences.defaultTodaySections),
            statsOrder = normalizeOrderedList(
                value.statsOrder,
                DailyRebuildPreferences.defaultStatsOrder
            ),
            hiddenStatsSections = value.hiddenStatsSections
                .intersect(DailyRebuildPreferences.defaultStatsOrder.toSet()),
            statsDefaultRange = value.statsDefaultRange
                .takeIf { it in VALID_STATS_RANGES } ?: "LAST_30_DAYS",
            meetingReminderLeadMinutes = value.meetingReminderLeadMinutes
                .takeIf { it in VALID_REMINDER_LEAD_MINUTES } ?: 60,
            iopReminderLeadMinutes = value.iopReminderLeadMinutes
                .takeIf { it in VALID_REMINDER_LEAD_MINUTES } ?: 60,
            notificationSnoozeMinutes = value.notificationSnoozeMinutes
                .takeIf { it in VALID_SNOOZE_MINUTES } ?: 15,
            weightUnit = value.weightUnit.takeIf { it in VALID_WEIGHT_UNITS } ?: "lb",
            distanceUnit = value.distanceUnit.takeIf { it in VALID_DISTANCE_UNITS } ?: "mi",
            waterUnit = value.waterUnit.takeIf { it in VALID_WATER_UNITS } ?: "oz",
            temperatureUnit = value.temperatureUnit
                .takeIf { it in VALID_TEMPERATURE_UNITS } ?: "f",
            foodMassUnit = value.foodMassUnit
                .takeIf { it in VALID_FOOD_MASS_UNITS } ?: "oz",
            heightUnit = value.heightUnit.takeIf { it in VALID_HEIGHT_UNITS } ?: "ft_in"
        )
    }

    private fun normalizeRecentSearches(values: List<String>): List<String> =
        values.asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .map { it.take(MAX_PORTABLE_TEXT_LENGTH) }
            .distinctBy { it.lowercase() }
            .take(MAX_RECENT_SEARCHES)
            .toList()

    private fun normalizeIgnoredSignatures(values: Set<String>): Set<String> =
        values.asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .map { it.take(MAX_PORTABLE_SIGNATURE_LENGTH) }
            .distinct()
            .take(MAX_IGNORED_SIGNATURES)
            .toCollection(linkedSetOf())

    private fun readSet(
        key: String,
        defaultValue: Set<String>
    ): Set<String> {
        return preferences.getStringSet(key, null)
            ?.toSet()
            ?: defaultValue.toSet()
    }

    private fun readOrderedList(
        key: String,
        defaultValue: List<String>
    ): List<String> {
        val stored = preferences.getString(key, null)
            ?.split(SEPARATOR)
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            .orEmpty()

        return normalizeOrderedList(stored, defaultValue)
    }

    private fun normalizeOrderedList(
        stored: List<String>,
        defaultValue: List<String>
    ): List<String> {
        val validStored = stored.filter { it in defaultValue }.distinct()
        return validStored + defaultValue.filterNot(validStored::contains)
    }

    companion object {
        private const val FILE_NAME = "daily_rebuild_preferences"
        private const val SEPARATOR = ","

        private const val KEY_ENABLED_LOG_SECTIONS = "enabled_log_sections"
        private const val KEY_QUICK_LOG_ORDER = "quick_log_order"
        private const val KEY_HIDDEN_QUICK_LOG_ACTIONS = "hidden_quick_log_actions"
        private const val KEY_VISIBLE_TODAY_SECTIONS = "visible_today_sections"
        private const val KEY_STATS_ORDER = "stats_order"
        private const val KEY_HIDDEN_STATS_SECTIONS = "hidden_stats_sections"
        private const val KEY_STATS_DEFAULT_RANGE = "stats_default_range"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_APPOINTMENT_REMINDERS = "appointment_reminders_enabled"
        private const val KEY_MEETING_REMINDERS = "meeting_reminders_enabled"
        private const val KEY_IOP_REMINDERS = "iop_reminders_enabled"
        private const val KEY_IOP_ATTENDANCE_FOLLOW_UP =
            "iop_attendance_follow_up_enabled"
        private const val KEY_MEETING_REMINDER_LEAD_MINUTES =
            "meeting_reminder_lead_minutes"
        private const val KEY_IOP_REMINDER_LEAD_MINUTES =
            "iop_reminder_lead_minutes"
        private const val KEY_NOTIFICATION_SNOOZE_MINUTES =
            "notification_snooze_minutes"
        private const val KEY_WEIGHT_UNIT = "weight_unit"
        private const val KEY_DISTANCE_UNIT = "distance_unit"
        private const val KEY_WATER_UNIT = "water_unit"
        private const val KEY_TEMPERATURE_UNIT = "temperature_unit"
        private const val KEY_FOOD_MASS_UNIT = "food_mass_unit"
        private const val KEY_HEIGHT_UNIT = "height_unit"
        private const val KEY_IOP_DEFAULTS_INITIALIZED = "iop_defaults_initialized"
        private const val KEY_TODAY_PHASE2_INITIALIZED = "today_phase2_initialized"

        private const val KEY_RECENT_SEARCHES = "recent_searches"
        private const val KEY_IGNORED_DATA_QUALITY_SIGNATURES =
            "ignored_data_quality_signatures"
        private const val RECENT_SEARCH_SEPARATOR = "\n"
        private const val MAX_RECENT_SEARCHES = 8
        private const val MAX_IGNORED_SIGNATURES = 500
        private const val MAX_PORTABLE_TEXT_LENGTH = 200
        private const val MAX_PORTABLE_SIGNATURE_LENGTH = 500

        private val VALID_STATS_RANGES = setOf(
            "LAST_7_DAYS",
            "LAST_30_DAYS",
            "LAST_90_DAYS",
            "CUSTOM",
            "ALL_TIME"
        )
        private val VALID_REMINDER_LEAD_MINUTES = setOf(15, 30, 60, 1_440)
        private val VALID_SNOOZE_MINUTES = setOf(15, 30, 60)
        private val VALID_WEIGHT_UNITS = setOf("lb", "kg")
        private val VALID_DISTANCE_UNITS = setOf("mi", "km")
        private val VALID_WATER_UNITS = setOf("oz", "ml")
        private val VALID_TEMPERATURE_UNITS = setOf("f", "c")
        private val VALID_FOOD_MASS_UNITS = setOf("oz", "g")
        private val VALID_HEIGHT_UNITS = setOf("ft_in", "cm")
    }
}
