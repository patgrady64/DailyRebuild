package com.pgdevhouse.dailyrebuild.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/**
 * Owns Daily Rebuild's simplified five-destination navigation.
 *
 * Main destinations are Today, Log, History, Insights, and More. Feature
 * screens can still remember their own subsection, but the primary navigation
 * no longer exposes planning, health, and settings as competing top-level hubs.
 */
class AppNavigationViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var selectedMainTab by mutableIntStateOf(
        savedStateHandle[KEY_MAIN_TAB] ?: TODAY_TAB
    )
        private set

    var selectedLogSection by mutableIntStateOf(
        savedStateHandle[KEY_LOG_SECTION] ?: LOG_HOME_SECTION
    )
        private set

    var selectedPlanSection by mutableIntStateOf(
        savedStateHandle[KEY_PLAN_SECTION] ?: PLAN_MEALS_SECTION
    )
        private set

    var selectedMobilitySection by mutableIntStateOf(
        savedStateHandle[KEY_MOBILITY_SECTION] ?: MOBILITY_TODAY_SECTION
    )
        private set

    var selectedMeetingsSection by mutableIntStateOf(
        savedStateHandle[KEY_MEETINGS_SECTION] ?: MEETINGS_ATTENDANCE_SECTION
    )
        private set

    fun selectMainTab(index: Int) {
        selectedMainTab = index.coerceIn(TODAY_TAB, MORE_TAB)
        savedStateHandle[KEY_MAIN_TAB] = selectedMainTab
    }

    fun selectLogSection(index: Int) {
        selectedLogSection = index.coerceIn(LOG_FOOD_SECTION, LOG_HOME_SECTION)
        savedStateHandle[KEY_LOG_SECTION] = selectedLogSection
    }

    fun openLogHome() {
        selectLogSection(LOG_HOME_SECTION)
        selectMainTab(LOG_TAB)
    }

    fun selectPlanSection(index: Int) {
        selectedPlanSection = index.coerceIn(PLAN_MEALS_SECTION, PLAN_APPOINTMENTS_SECTION)
        savedStateHandle[KEY_PLAN_SECTION] = selectedPlanSection
    }

    fun selectMobilitySection(index: Int) {
        selectedMobilitySection = index.coerceIn(MOBILITY_TODAY_SECTION, MOBILITY_HISTORY_SECTION)
        savedStateHandle[KEY_MOBILITY_SECTION] = selectedMobilitySection
    }

    fun selectMeetingsSection(index: Int) {
        selectedMeetingsSection = index.coerceIn(MEETINGS_ATTENDANCE_SECTION, MEETINGS_IOP_SECTION)
        savedStateHandle[KEY_MEETINGS_SECTION] = selectedMeetingsSection
    }

    fun openLogSection(section: Int) {
        selectLogSection(section)
        selectMainTab(LOG_TAB)
    }

    /**
     * Compatibility routing for older feature callbacks. Planning destinations
     * now live under Log or More rather than occupying a main navigation tab.
     */
    fun openPlanSection(section: Int) {
        selectPlanSection(section)
        when (section) {
            PLAN_MEALS_SECTION -> openLogSection(LOG_FOOD_SECTION)
            PLAN_PANTRY_SECTION,
            PLAN_SHOP_SECTION -> selectMainTab(MORE_TAB)
            PLAN_APPOINTMENTS_SECTION -> selectMainTab(MORE_TAB)
        }
    }

    fun openIopGroups() {
        selectMeetingsSection(MEETINGS_IOP_SECTION)
        openLogSection(LOG_MEETINGS_SECTION)
    }

    fun returnToToday(): Boolean {
        if (selectedMainTab == TODAY_TAB) return false
        selectMainTab(TODAY_TAB)
        return true
    }

    companion object {
        const val TODAY_TAB = 0
        const val LOG_TAB = 1
        const val HISTORY_TAB = 2
        const val INSIGHTS_TAB = 3
        const val MORE_TAB = 4

        // Compatibility aliases retained for feature code that still refers to
        // the previous destination names.
        const val PLAN_TAB = HISTORY_TAB
        const val STATS_TAB = INSIGHTS_TAB
        const val HEALTH_TAB = MORE_TAB

        const val LOG_FOOD_SECTION = 0
        const val LOG_MOVEMENT_SECTION = 1
        const val LOG_MEETINGS_SECTION = 2
        const val LOG_HEALTH_SECTION = 3
        const val LOG_MAINTENANCE_SECTION = 4
        const val LOG_HOME_SECTION = 5

        const val PLAN_MEALS_SECTION = 0
        const val PLAN_PANTRY_SECTION = 1
        const val PLAN_SHOP_SECTION = 2
        const val PLAN_APPOINTMENTS_SECTION = 3

        const val MOBILITY_TODAY_SECTION = 0
        const val MOBILITY_ROUTINES_SECTION = 1
        const val MOBILITY_HISTORY_SECTION = 2

        const val MEETINGS_ATTENDANCE_SECTION = 0
        const val MEETINGS_IOP_SECTION = 1

        private const val KEY_MAIN_TAB = "selected_main_tab"
        private const val KEY_LOG_SECTION = "selected_log_section"
        private const val KEY_PLAN_SECTION = "selected_plan_section"
        private const val KEY_MOBILITY_SECTION = "selected_mobility_section"
        private const val KEY_MEETINGS_SECTION = "selected_meetings_section"
    }
}
