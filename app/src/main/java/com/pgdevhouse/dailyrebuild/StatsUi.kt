package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferences
import com.pgdevhouse.dailyrebuild.ui.stats.StatsChart
import com.pgdevhouse.dailyrebuild.ui.stats.StatsChartType
import com.pgdevhouse.dailyrebuild.ui.stats.StatsFilter
import com.pgdevhouse.dailyrebuild.ui.stats.StatsListItem
import com.pgdevhouse.dailyrebuild.ui.stats.StatsMetric
import com.pgdevhouse.dailyrebuild.ui.stats.StatsPoint
import com.pgdevhouse.dailyrebuild.ui.stats.StatsRange
import com.pgdevhouse.dailyrebuild.ui.stats.StatsSection
import com.pgdevhouse.dailyrebuild.ui.stats.StatsUiState
import java.time.LocalDate

@Composable
fun StatsScreen(
    state: StatsUiState,
    preferences: DailyRebuildPreferences,
    onRangeSelected: (StatsRange) -> Unit,
    onCustomRangeSelected: (LocalDate, LocalDate) -> Unit,
    onFilterSelected: (StatsFilter) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onRefresh: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenHistoryDate: (String) -> Unit,
    onOpenSearch: (() -> Unit)? = null,
    dataQualityWarnings: List<DataQualityWarning> = emptyList(),
    onReviewDataQualityWarning: (DataQualityWarning) -> Unit = {},
    onKeepDataQualityWarning: (DataQualityWarning) -> Unit = {},
    onIgnoreDataQualityWarning: (DataQualityWarning) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCustomRange by rememberSaveable { mutableStateOf(false) }
    val visibleFilters = remember(
        preferences.statsOrder,
        preferences.hiddenStatsSections
    ) {
        val ordered = preferences.statsOrder.mapNotNull { preferenceId ->
            StatsFilter.entries.firstOrNull { it.preferenceId == preferenceId }
        }.filterNot { it.preferenceId in preferences.hiddenStatsSections }
        listOf(StatsFilter.OVERVIEW) + ordered
    }
    val activeFilter = state.selectedFilter.takeIf { it in visibleFilters }
        ?: StatsFilter.OVERVIEW
    val section = state.sections[activeFilter] ?: StatsSection()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubScreenHeader(
            title = "Insights",
            subtitle = "Steps, trends, comparisons, and records that may need review.",
            onOpenHistory = onOpenHistory,
            onOpenSearch = onOpenSearch
        )

        DataQualitySummaryCard(
            warnings = dataQualityWarnings,
            onReview = onReviewDataQualityWarning,
            onKeep = onKeepDataQualityWarning,
            onIgnoreExactValue = onIgnoreDataQualityWarning
        )

        StatsChipGrid(
            labels = StatsRange.entries.map(StatsRange::label),
            selectedIndex = StatsRange.entries.indexOf(state.selectedRange),
            columns = 3,
            onSelected = { index ->
                val range = StatsRange.entries[index]
                if (range == StatsRange.CUSTOM) {
                    showCustomRange = true
                } else {
                    onRangeSelected(range)
                }
            }
        )

        StatsPeriodNavigator(
            label = state.periodLabel,
            canMoveNext = state.canMoveNext,
            onPrevious = onPreviousPeriod,
            onNext = onNextPeriod
        )

        if (state.comparisonPeriodLabel.isNotBlank()) {
            Text(
                text = "Comparisons use ${state.comparisonPeriodLabel}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            state.isLoading -> Unit
            state.errorMessage != null -> Unit
            else -> DailyStepsPanel(
                points = state.dailySteps,
                periodLabel = state.periodLabel,
                onOpenHistoryDate = onOpenHistoryDate
            )
        }

        StatsChipGrid(
            labels = visibleFilters.map(StatsFilter::label),
            selectedIndex = visibleFilters.indexOf(activeFilter),
            columns = 2,
            onSelected = { onFilterSelected(visibleFilters[it]) }
        )

        when {
            state.isLoading -> StatsLoadingPanel()
            state.errorMessage != null -> StatsErrorPanel(
                message = state.errorMessage,
                onRetry = onRefresh
            )
            section.isEmpty -> RebuildInsetPanel {
                Text("No records are available for this category in the selected period.")
                Text(
                    "Try another date range or show a different Insights category in Customize Daily Rebuild.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                StatsMetricGrid(section.metrics)

                if (section.highlights.isNotEmpty()) {
                    StatsHighlights(
                        items = section.highlights,
                        onOpenHistoryDate = onOpenHistoryDate
                    )
                }

                section.charts.forEach { chart ->
                    InteractiveStatsChart(
                        chart = chart,
                        onOpenHistoryDate = onOpenHistoryDate
                    )
                }

                section.notes.forEach { note ->
                    RebuildInsetPanel {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }

    if (showCustomRange) {
        StatsCustomRangeDialog(
            initialStart = state.customStartDate,
            initialEnd = state.customEndDate,
            onSave = { start, end ->
                onCustomRangeSelected(start, end)
                showCustomRange = false
            },
            onDismiss = { showCustomRange = false }
        )
    }
}

@Composable
private fun DailyStepsPanel(
    points: List<StatsPoint>,
    periodLabel: String,
    onOpenHistoryDate: (String) -> Unit
) {
    RebuildSectionCard(
        title = "Steps by day",
        subtitle = if (periodLabel.isBlank()) {
            "Every date in the selected period is shown."
        } else {
            "$periodLabel · Every date is shown."
        },
        accentColor = RebuildBlue
    ) {
        if (points.isEmpty()) {
            Text(
                text = "No dates are available for this period.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Every selected day has a step count. When Fit has no data for a day, Daily Rebuild treats that day as 0 steps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = points,
                    key = { it.historyDate ?: it.label }
                ) { point ->
                    Surface(
                        onClick = {
                            point.historyDate?.let(onOpenHistoryDate)
                        },
                        modifier = Modifier.width(126.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (point.hasData) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = point.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = point.valueText,
                                style = if (point.hasData) {
                                    MaterialTheme.typography.titleMedium
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                },
                                fontWeight = if (point.hasData) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = "Open date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsHighlights(
    items: List<StatsListItem>,
    onOpenHistoryDate: (String) -> Unit
) {
    RebuildSectionCard(
        title = "Highlights",
        subtitle = "Useful details from the selected period.",
        accentColor = RebuildAmber
    ) {
        items.forEach { item ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            item.value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (item.detail.isNotBlank()) {
                            Text(
                                item.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    item.historyDate?.let { date ->
                        TextButton(onClick = { onOpenHistoryDate(date) }) {
                            Text("Open")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCustomRangeDialog(
    initialStart: LocalDate,
    initialEnd: LocalDate,
    onSave: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var startText by rememberSaveable { mutableStateOf(initialStart.toString()) }
    var endText by rememberSaveable { mutableStateOf(initialEnd.toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose custom date range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Use YYYY-MM-DD. The end date cannot be in the future.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = startText,
                    onValueChange = {
                        startText = it
                        error = null
                    },
                    label = { Text("Start date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = {
                        endText = it
                        error = null
                    },
                    label = { Text("End date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val start = runCatching { LocalDate.parse(startText.trim()) }.getOrNull()
                    val end = runCatching { LocalDate.parse(endText.trim()) }.getOrNull()
                    error = when {
                        start == null || end == null -> "Enter both dates in YYYY-MM-DD format."
                        end.isBefore(start) -> "The end date must be on or after the start date."
                        end.isAfter(LocalDate.now()) -> "The end date cannot be in the future."
                        else -> null
                    }
                    if (error == null && start != null && end != null) {
                        onSave(start, end)
                    }
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun StatsPeriodNavigator(
    label: String,
    canMoveNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onPrevious) {
                Text("‹ Previous")
            }
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = onNext,
                enabled = canMoveNext
            ) {
                Text("Next ›")
            }
        }
    }
}

@Composable
private fun StatsMetricGrid(
    metrics: List<StatsMetric>
) {
    metrics.chunked(2).forEach { rowMetrics ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rowMetrics.forEach { metric ->
                StatsMetricCard(
                    metric = metric,
                    modifier = Modifier.weight(1f)
                )
            }
            if (rowMetrics.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatsMetricCard(
    metric: StatsMetric,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = metric.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (metric.detail.isNotBlank()) {
                Text(
                    text = metric.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (metric.comparison.isNotBlank()) {
                Text(
                    text = metric.comparison,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InteractiveStatsChart(
    chart: StatsChart,
    onOpenHistoryDate: (String) -> Unit
) {
    var selectedPoint by remember(chart.title, chart.points) {
        mutableStateOf<StatsPoint?>(null)
    }

    RebuildSectionCard(
        title = chart.title,
        subtitle = chart.subtitle,
        accentColor = if (chart.type == StatsChartType.LINE) RebuildBlue else RebuildGreen
    ) {
        val availablePoints = chart.points.filter { it.hasData }

        if (availablePoints.isEmpty()) {
            Text(
                text = "No data was recorded for this chart in the selected period.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            selectedPoint?.let { point ->
                StatsPointDetail(
                    point = point,
                    onOpenHistoryDate = onOpenHistoryDate
                )
            }

            StatsChartCanvas(
                chart = chart,
                selectedPoint = selectedPoint,
                onPointSelected = { selectedPoint = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    chart.points.firstOrNull()?.label.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Tap a point or bar for its exact value",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    chart.points.lastOrNull()?.label.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatsPointDetail(
    point: StatsPoint,
    onOpenHistoryDate: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(point.label, fontWeight = FontWeight.SemiBold)
                Text(
                    point.valueText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            point.historyDate?.let { date ->
                OutlinedButton(onClick = { onOpenHistoryDate(date) }) {
                    Text("Open Date")
                }
            }
        }
    }
}

@Composable
private fun StatsChartCanvas(
    chart: StatsChart,
    selectedPoint: StatsPoint?,
    onPointSelected: (StatsPoint) -> Unit
) {
    val points = chart.points
    val chartWidth = maxOf(330, points.size * 34).dp
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val missing = MaterialTheme.colorScheme.surfaceVariant
    val selected = MaterialTheme.colorScheme.tertiary
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        Canvas(
            modifier = Modifier
                .width(chartWidth)
                .height(190.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.isEmpty()) return@detectTapGestures
                        val segmentWidth = size.width / points.size.coerceAtLeast(1)
                        val index = (offset.x / segmentWidth)
                            .toInt()
                            .coerceIn(0, points.lastIndex)
                        onPointSelected(points[index])
                    }
                }
        ) {
            val left = 12.dp.toPx()
            val right = size.width - 12.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = size.height - 18.dp.toPx()
            val usableWidth = (right - left).coerceAtLeast(1f)
            val usableHeight = (bottom - top).coerceAtLeast(1f)
            val validValues = points.filter { it.hasData }.map { it.value }
            val rawMaximum = validValues.maxOrNull() ?: 1.0
            val rawMinimum = validValues.minOrNull() ?: 0.0
            val minimumValue = if (chart.type == StatsChartType.LINE && rawMinimum > 0.0) {
                val spread = (rawMaximum - rawMinimum).coerceAtLeast(1.0)
                (rawMinimum - spread * 0.15).coerceAtLeast(0.0)
            } else {
                0.0
            }
            val valueRange = (rawMaximum - minimumValue).coerceAtLeast(1.0)

            repeat(4) { lineIndex ->
                val y = top + usableHeight * lineIndex / 3f
                drawLine(
                    color = grid,
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (chart.type == StatsChartType.BAR) {
                val slotWidth = usableWidth / points.size.coerceAtLeast(1)
                val barWidth = (slotWidth * 0.62f).coerceAtLeast(4.dp.toPx())
                points.forEachIndexed { index, point ->
                    val xCenter = left + slotWidth * (index + 0.5f)
                    val barHeight = if (point.hasData) {
                        (point.value / rawMaximum.coerceAtLeast(1.0) * usableHeight)
                            .toFloat()
                            .coerceAtLeast(2.dp.toPx())
                    } else {
                        2.dp.toPx()
                    }
                    drawRoundRect(
                        color = when {
                            point == selectedPoint -> selected
                            point.hasData -> secondary
                            else -> missing
                        },
                        topLeft = Offset(xCenter - barWidth / 2f, bottom - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                    )
                }
            } else {
                val spacing = if (points.size <= 1) 0f else usableWidth / (points.size - 1)
                var previous: Pair<Offset, StatsPoint>? = null
                points.forEachIndexed { index, point ->
                    val x = left + spacing * index
                    if (!point.hasData) {
                        previous = null
                        return@forEachIndexed
                    }
                    val y = bottom - ((point.value - minimumValue) / valueRange * usableHeight).toFloat()
                    val current = Offset(x, y)
                    previous?.let { (previousOffset, _) ->
                        drawLine(
                            color = primary,
                            start = previousOffset,
                            end = current,
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                    drawCircle(
                        color = if (point == selectedPoint) selected else primary,
                        radius = if (point == selectedPoint) 6.dp.toPx() else 4.dp.toPx(),
                        center = current
                    )
                    if (point == selectedPoint) {
                        drawCircle(
                            color = selected,
                            radius = 9.dp.toPx(),
                            center = current,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    previous = current to point
                }
            }
        }
    }
}

@Composable
private fun StatsLoadingPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
        Text("Calculating statistics…")
    }
}

@Composable
private fun StatsErrorPanel(
    message: String,
    onRetry: () -> Unit
) {
    RebuildSectionCard(
        title = "Insights could not load",
        accentColor = MaterialTheme.colorScheme.error
    ) {
        Text(message)
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try Again")
        }
    }
}

@Composable
private fun StatsChipGrid(
    labels: List<String>,
    selectedIndex: Int,
    columns: Int = 2,
    onSelected: (Int) -> Unit
) {
    labels.chunked(columns).forEachIndexed { rowIndex, rowLabels ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowLabels.forEachIndexed { itemIndex, label ->
                val index = rowIndex * columns + itemIndex
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { onSelected(index) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(columns - rowLabels.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
