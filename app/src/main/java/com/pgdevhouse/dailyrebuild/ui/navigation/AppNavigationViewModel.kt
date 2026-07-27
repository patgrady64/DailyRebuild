package com.pgdevhouse.dailyrebuild.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/**
 * Owns durable app navigation independently from feature data and dialogs.
 * Android Back behavior is coordinated by the app shell, while the selected
 * hub and Food subsection survive configuration changes through SavedStateHandle.
 */
class AppNavigationViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var selectedMainTab by mutableIntStateOf(
        savedStateHandle[KEY_MAIN_TAB] ?: TODAY_TAB
    )
        private set

    var selectedFoodSection by mutableIntStateOf(
        savedStateHandle[KEY_FOOD_SECTION] ?: FOOD_TODAY_SECTION
    )
        private set

    var selectedMobilitySection by mutableIntStateOf(
        savedStateHandle[KEY_MOBILITY_SECTION] ?: MOBILITY_TODAY_SECTION
    )
        private set

    fun selectMainTab(index: Int) {
        selectedMainTab = index.coerceIn(TODAY_TAB, HEALTH_TAB)
        savedStateHandle[KEY_MAIN_TAB] = selectedMainTab
    }

    fun selectFoodSection(index: Int) {
        selectedFoodSection = index.coerceIn(FOOD_TODAY_SECTION, FOOD_SHOP_SECTION)
        savedStateHandle[KEY_FOOD_SECTION] = selectedFoodSection
    }

    fun selectMobilitySection(index: Int) {
        selectedMobilitySection = index.coerceIn(MOBILITY_TODAY_SECTION, MOBILITY_HISTORY_SECTION)
        savedStateHandle[KEY_MOBILITY_SECTION] = selectedMobilitySection
    }

    fun returnToToday(): Boolean {
        if (selectedMainTab == TODAY_TAB) return false
        selectMainTab(TODAY_TAB)
        return true
    }

    companion object {
        const val TODAY_TAB = 0
        const val FOOD_TAB = 1
        const val MOBILITY_TAB = 2
        const val MEETINGS_TAB = 3
        const val HEALTH_TAB = 4

        const val FOOD_TODAY_SECTION = 0
        const val FOOD_PLAN_SECTION = 1
        const val FOOD_PANTRY_SECTION = 2
        const val FOOD_SHOP_SECTION = 3

        const val MOBILITY_TODAY_SECTION = 0
        const val MOBILITY_ROUTINES_SECTION = 1
        const val MOBILITY_HISTORY_SECTION = 2

        private const val KEY_MAIN_TAB = "selected_main_tab"
        private const val KEY_FOOD_SECTION = "selected_food_section"
        private const val KEY_MOBILITY_SECTION = "selected_mobility_section"
    }
}
