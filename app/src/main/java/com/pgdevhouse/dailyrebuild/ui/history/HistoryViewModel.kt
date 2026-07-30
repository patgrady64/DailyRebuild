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
        val records = repositories.dailyRecords.getAllRecords()
        val foodEntries = repositories.food.getAllEntries()
        val activitySnapshots = repositories.activity.getAllSnapshots()
        val mobilitySessions = repositories.mobility.getAllSessions()
        val showerLogs = repositories.showers.getAllLogs()
        val migraineEvents = repositories.migraines.getAllLogs()
        val meetingAttendance = repositories.meetings.getAllAttendance()
        val careVisits = repositories.careVisits.getAllVisits()
        val careAppointments = repositories.appointments.getAllAppointments()
        val lifeMaintenanceLogs = repositories.lifeMaintenance.getAllLogs()

        val recordsByDate = records.associateBy { it.date }
        val foodByDate = foodEntries.groupBy { it.date }
        val activityByDate = activitySnapshots.associateBy { it.date }
        val mobilityByDate = mobilitySessions.groupBy { it.date }
        val showersByDate = showerLogs.associateBy { it.date }
        val migrainesByDate = migraineEvents.groupBy { it.date }
        val meetingsByDate = meetingAttendance.groupBy { it.date }
        val visitsByDate = careVisits.groupBy { it.date }
        val appointmentsByDate = careAppointments.groupBy { it.date }
        val lifeMaintenanceByDate = lifeMaintenanceLogs.groupBy { it.date }

        val allDates = buildSet {
            addAll(recordsByDate.keys)
            addAll(foodByDate.keys)
            addAll(activityByDate.keys)
            addAll(mobilityByDate.keys)
            addAll(showersByDate.keys)
            addAll(migrainesByDate.keys)
            addAll(meetingsByDate.keys)
            addAll(visitsByDate.keys)
            addAll(appointmentsByDate.keys)
            addAll(lifeMaintenanceByDate.keys)
        }

        return allDates.map { date ->
            DailyHistoryDay(
                date = date,
                record = recordsByDate[date],
                foodEntries = foodByDate[date].orEmpty(),
                activitySnapshot = activityByDate[date],
                mobilitySessions = mobilityByDate[date].orEmpty(),
                showerLogged = showersByDate.containsKey(date),
                migraineLogs = migrainesByDate[date].orEmpty(),
                meetingAttendance = meetingsByDate[date].orEmpty(),
                careVisits = visitsByDate[date].orEmpty(),
                careAppointments = appointmentsByDate[date].orEmpty(),
                lifeMaintenanceLogs = lifeMaintenanceByDate[date].orEmpty()
            )
        }.sortedByDescending { it.date }
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
