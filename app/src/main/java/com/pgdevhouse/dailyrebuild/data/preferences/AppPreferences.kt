package com.pgdevhouse.dailyrebuild.data.preferences

import android.content.Context

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
    val appointmentRemindersEnabled: Boolean = true,
    val meetingRemindersEnabled: Boolean = false,
    val iopRemindersEnabled: Boolean = false,
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
            ),
            visibleTodaySections = visibleTodaySections,
            statsOrder = readOrderedList(
                KEY_STATS_ORDER,
                DailyRebuildPreferences.defaultStatsOrder
            ),
            hiddenStatsSections = readSet(
                KEY_HIDDEN_STATS_SECTIONS,
                emptySet()
            ),
            statsDefaultRange = preferences.getString(
                KEY_STATS_DEFAULT_RANGE,
                "LAST_30_DAYS"
            ) ?: "LAST_30_DAYS",
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
            weightUnit = preferences.getString(KEY_WEIGHT_UNIT, "lb") ?: "lb",
            distanceUnit = preferences.getString(KEY_DISTANCE_UNIT, "mi") ?: "mi",
            waterUnit = preferences.getString(KEY_WATER_UNIT, "oz") ?: "oz",
            temperatureUnit = preferences.getString(KEY_TEMPERATURE_UNIT, "f") ?: "f",
            foodMassUnit = preferences.getString(KEY_FOOD_MASS_UNIT, "oz") ?: "oz",
            heightUnit = preferences.getString(KEY_HEIGHT_UNIT, "ft_in") ?: "ft_in"
        )
    }

    fun save(value: DailyRebuildPreferences) {
        preferences.edit()
            .putStringSet(KEY_ENABLED_LOG_SECTIONS, value.enabledLogSections)
            .putString(KEY_QUICK_LOG_ORDER, value.quickLogOrder.joinToString(SEPARATOR))
            .putStringSet(KEY_HIDDEN_QUICK_LOG_ACTIONS, value.hiddenQuickLogActions)
            .putStringSet(KEY_VISIBLE_TODAY_SECTIONS, value.visibleTodaySections)
            .putString(KEY_STATS_ORDER, value.statsOrder.joinToString(SEPARATOR))
            .putStringSet(KEY_HIDDEN_STATS_SECTIONS, value.hiddenStatsSections)
            .putString(KEY_STATS_DEFAULT_RANGE, value.statsDefaultRange)
            .putBoolean(KEY_APPOINTMENT_REMINDERS, value.appointmentRemindersEnabled)
            .putBoolean(KEY_MEETING_REMINDERS, value.meetingRemindersEnabled)
            .putBoolean(KEY_IOP_REMINDERS, value.iopRemindersEnabled)
            .putString(KEY_WEIGHT_UNIT, value.weightUnit)
            .putString(KEY_DISTANCE_UNIT, value.distanceUnit)
            .putString(KEY_WATER_UNIT, value.waterUnit)
            .putString(KEY_TEMPERATURE_UNIT, value.temperatureUnit)
            .putString(KEY_FOOD_MASS_UNIT, value.foodMassUnit)
            .putString(KEY_HEIGHT_UNIT, value.heightUnit)
            .apply()
    }


    fun loadRecentSearches(): List<String> {
        return preferences.getString(KEY_RECENT_SEARCHES, null)
            ?.split(RECENT_SEARCH_SEPARATOR)
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.distinct()
            ?.take(MAX_RECENT_SEARCHES)
            .orEmpty()
    }

    fun rememberSearch(query: String): List<String> {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return loadRecentSearches()

        val updated = (listOf(cleaned) + loadRecentSearches()
            .filterNot { it.equals(cleaned, ignoreCase = true) })
            .take(MAX_RECENT_SEARCHES)

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
        preferences.getStringSet(KEY_IGNORED_DATA_QUALITY_SIGNATURES, emptySet())
            ?.toSet()
            .orEmpty()

    fun ignoreDataQualitySignature(signature: String) {
        if (signature.isBlank()) return
        val updated = loadIgnoredDataQualitySignatures() + signature
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

        val validStored = stored.filter { it in defaultValue }
        return (validStored + defaultValue.filterNot(validStored::contains)).distinct()
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
        private const val KEY_APPOINTMENT_REMINDERS = "appointment_reminders_enabled"
        private const val KEY_MEETING_REMINDERS = "meeting_reminders_enabled"
        private const val KEY_IOP_REMINDERS = "iop_reminders_enabled"
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
    }
}
