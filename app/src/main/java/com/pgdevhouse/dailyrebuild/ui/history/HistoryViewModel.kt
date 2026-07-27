package com.pgdevhouse.dailyrebuild.ui.history

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pgdevhouse.dailyrebuild.DailyHistoryDay
import com.pgdevhouse.dailyrebuild.DailyHistoryFilter
import com.pgdevhouse.dailyrebuild.data.repository.DailyRebuildRepositories
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HistoryUiState(
    val days: List<DailyHistoryDay> = emptyList(),
    val selectedFilter: DailyHistoryFilter = DailyHistoryFilter.ALL,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null
)

/** Owns global calendar loading, filtering input data, and whole-day deletion. */
class HistoryViewModel(
    private val repositories: DailyRebuildRepositories
) : ViewModel() {

    var state by mutableStateOf(HistoryUiState())
        private set

    fun refresh(onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                state = state.copy(days = loadHistoryDays(), isLoading = false, errorMessage = null)
                onResult(null)
            } catch (exception: Exception) {
                Log.e("DailyRebuildHistory", "Could not load history", exception)
                val message = "Could not load daily history."
                state = state.copy(isLoading = false, errorMessage = message)
                onResult(message)
            }
        }
    }


    fun selectFilter(filter: DailyHistoryFilter) {
        state = state.copy(selectedFilter = filter)
    }

    fun deleteDay(
        day: DailyHistoryDay,
        onResult: (String, Boolean) -> Unit
    ) {
        viewModelScope.launch {
            state = state.copy(isDeleting = true, errorMessage = null)
            try {
                repositories.history.deleteDate(day.date)
                state = state.copy(
                    days = state.days.filterNot { it.date == day.date },
                    isDeleting = false
                )
                onResult("Day deleted.", true)
            } catch (exception: Exception) {
                Log.e("DailyRebuildHistory", "Could not delete ${day.date}", exception)
                val message = "Could not delete that day."
                state = state.copy(isDeleting = false, errorMessage = message)
                onResult(message, false)
            }
        }
    }

    private suspend fun loadHistoryDays(): List<DailyHistoryDay> {
        val loadedDays = mutableListOf<DailyHistoryDay>()
        val today = LocalDate.now()
        val appointmentHistory = repositories.appointments.getAllAppointments()
        val appointmentsByDate = appointmentHistory.groupBy { it.date }

        for (dayOffset in 0L until 365L) {
            val date = today.minusDays(dayOffset).toString()
            val record = repositories.dailyRecords.getRecordByDate(date)
            val entries = repositories.food.getEntriesForDate(date)
            val activitySnapshot = repositories.activity.getSnapshotByDate(date)
            val mobilitySessions = repositories.mobility.getSessionsForDate(date)
            val showerLog = repositories.showers.getLogByDate(date)
            val migraineEvents = repositories.migraines.getLogsForDate(date)
            val meetingAttendance = repositories.meetings.getAttendanceForDate(date)
            val careVisits = repositories.careVisits.getVisitsForDate(date)
            val careAppointments = appointmentsByDate[date].orEmpty()

            if (
                record != null ||
                entries.isNotEmpty() ||
                activitySnapshot != null ||
                mobilitySessions.isNotEmpty() ||
                showerLog != null ||
                migraineEvents.isNotEmpty() ||
                meetingAttendance.isNotEmpty() ||
                careVisits.isNotEmpty() ||
                careAppointments.isNotEmpty()
            ) {
                loadedDays += DailyHistoryDay(
                    date = date,
                    record = record,
                    foodEntries = entries,
                    activitySnapshot = activitySnapshot,
                    mobilitySessions = mobilitySessions,
                    showerLogged = showerLog != null,
                    migraineLogs = migraineEvents,
                    meetingAttendance = meetingAttendance,
                    careVisits = careVisits,
                    careAppointments = careAppointments
                )
            }
        }

        val futureLimit = today.plusYears(1).toString()
        appointmentsByDate
            .filterKeys { date ->
                date > today.toString() && date <= futureLimit
            }
            .forEach { (date, appointments) ->
                loadedDays += DailyHistoryDay(
                    date = date,
                    record = null,
                    foodEntries = emptyList(),
                    careAppointments = appointments
                )
            }

        return loadedDays.sortedByDescending { it.date }
    }

    companion object {
        fun factory(
            repositories: DailyRebuildRepositories
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(repositories) as T
                }
            }
        }
    }
}
