package com.pgdevhouse.dailyrebuild.ui.stats

import com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferenceIds
import java.time.LocalDate

enum class StatsRange(
    val label: String,
    val dayCount: Int?
) {
    LAST_7_DAYS("7 Days", 7),
    LAST_30_DAYS("30 Days", 30),
    LAST_90_DAYS("90 Days", 90),
    CUSTOM("Custom", null),
    ALL_TIME("All Time", null)
}

enum class StatsFilter(
    val label: String,
    val preferenceId: String?
) {
    OVERVIEW("Overview", null),
    NUTRITION("Nutrition", DailyRebuildPreferenceIds.STATS_NUTRITION),
    WATER("Water", DailyRebuildPreferenceIds.STATS_WATER),
    PAIN("Pain", DailyRebuildPreferenceIds.STATS_PAIN),
    MOBILITY("Mobility", DailyRebuildPreferenceIds.STATS_MOBILITY),
    MEETINGS("Meetings & IOP", DailyRebuildPreferenceIds.STATS_MEETINGS),
    HEALTH("Health", DailyRebuildPreferenceIds.STATS_HEALTH),
    MIGRAINE("Migraine / Aura", DailyRebuildPreferenceIds.STATS_MIGRAINE),
    MAINTENANCE("Maintenance", DailyRebuildPreferenceIds.STATS_MAINTENANCE)
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

data class StatsListItem(
    val label: String,
    val value: String,
    val detail: String = "",
    val historyDate: String? = null
)

data class StatsSection(
    val metrics: List<StatsMetric> = emptyList(),
    val charts: List<StatsChart> = emptyList(),
    val highlights: List<StatsListItem> = emptyList(),
    val notes: List<String> = emptyList()
) {
    val isEmpty: Boolean
        get() = metrics.isEmpty() && charts.isEmpty() && highlights.isEmpty()
}

data class StatsUiState(
    val selectedRange: StatsRange = StatsRange.LAST_30_DAYS,
    val selectedFilter: StatsFilter = StatsFilter.OVERVIEW,
    val anchorDate: LocalDate = LocalDate.now(),
    val customStartDate: LocalDate = LocalDate.now().minusDays(29),
    val customEndDate: LocalDate = LocalDate.now(),
    val periodLabel: String = "",
    val comparisonPeriodLabel: String = "",
    val canMoveNext: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val sections: Map<StatsFilter, StatsSection> = emptyMap()
) {
    val selectedSection: StatsSection
        get() = sections[selectedFilter] ?: StatsSection()
}
