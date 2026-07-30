package com.pgdevhouse.dailyrebuild.ui.stats

import java.time.LocalDate

enum class StatsRange(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL_TIME("All Time")
}

enum class StatsFilter(val label: String) {
    OVERVIEW("Overview"),
    FOOD("Food"),
    MOVEMENT("Movement"),
    MEETINGS("Meetings"),
    HEALTH("Health"),
    SELF_CARE("Self-care"),
    LAST_COMPLETED("Last Time Completed")
}

enum class StatsChartType {
    LINE,
    BAR
}

data class StatsMetric(
    val label: String,
    val value: String,
    val detail: String = "",
    val comparison: String = ""
)

data class StatsPoint(
    val label: String,
    val value: Double,
    val valueText: String,
    val historyDate: String? = null,
    val hasData: Boolean = true
)

data class StatsChart(
    val title: String,
    val subtitle: String,
    val type: StatsChartType,
    val points: List<StatsPoint>
)

data class StatsSection(
    val metrics: List<StatsMetric> = emptyList(),
    val charts: List<StatsChart> = emptyList(),
    val notes: List<String> = emptyList()
)

data class StatsUiState(
    val selectedRange: StatsRange = StatsRange.WEEK,
    val selectedFilter: StatsFilter = StatsFilter.OVERVIEW,
    val anchorDate: LocalDate = LocalDate.now(),
    val periodLabel: String = "",
    val canMoveNext: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val sections: Map<StatsFilter, StatsSection> = emptyMap()
) {
    val selectedSection: StatsSection
        get() = sections[selectedFilter] ?: StatsSection()
}
