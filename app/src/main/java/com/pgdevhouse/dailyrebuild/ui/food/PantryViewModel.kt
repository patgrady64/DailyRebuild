package com.pgdevhouse.dailyrebuild.ui.food

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pgdevhouse.dailyrebuild.data.local.PantryEssential
import com.pgdevhouse.dailyrebuild.data.repository.PantryRepository
import kotlinx.coroutines.launch

data class PantryUiState(
    val items: List<PantryEssential> = emptyList(),
    val isWorking: Boolean = false,
    val errorMessage: String? = null
)

/** Feature owner for Pantry and required Shop items. */
class PantryViewModel(
    private val repository: PantryRepository
) : ViewModel() {

    var state by mutableStateOf(PantryUiState())
        private set

    suspend fun refresh() {
        state = state.copy(isWorking = true, errorMessage = null)
        try {
            state = PantryUiState(items = repository.getAll())
        } catch (exception: Exception) {
            state = state.copy(
                isWorking = false,
                errorMessage = "Could not load Pantry Essentials."
            )
            throw exception
        }
    }

    fun save(
        item: PantryEssential,
        onResult: (String) -> Unit
    ) = runOperation(
        successMessage = "Pantry essential saved.",
        failureMessage = "Could not save pantry essential.",
        onResult = onResult
    ) {
        if (item.id == 0L) repository.insert(item) else repository.update(item)
    }

    fun delete(
        item: PantryEssential,
        onResult: (String) -> Unit
    ) = runOperation(
        successMessage = "Pantry essential deleted.",
        failureMessage = "Could not delete pantry essential.",
        onResult = onResult
    ) {
        repository.delete(item)
    }

    fun updateStatus(
        item: PantryEssential,
        status: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            state = state.copy(isWorking = true, errorMessage = null)
            try {
                repository.updateStatus(
                    id = item.id,
                    status = status,
                    updatedAt = System.currentTimeMillis()
                )
                state = PantryUiState(items = repository.getAll())
                onResult(null)
            } catch (exception: Exception) {
                val message = "Could not update pantry status."
                state = state.copy(isWorking = false, errorMessage = message)
                onResult(message)
            }
        }
    }

    fun markNeededPurchased(
        onResult: (String) -> Unit
    ) = runOperation(
        successMessage = "Required pantry items marked Have.",
        failureMessage = "Could not update pantry items.",
        onResult = onResult
    ) {
        repository.markAllNeededAsHave(System.currentTimeMillis())
    }

    private fun runOperation(
        successMessage: String,
        failureMessage: String,
        onResult: (String) -> Unit,
        operation: suspend () -> Unit
    ) {
        viewModelScope.launch {
            state = state.copy(isWorking = true, errorMessage = null)
            try {
                operation()
                state = PantryUiState(items = repository.getAll())
                onResult(successMessage)
            } catch (exception: Exception) {
                state = state.copy(isWorking = false, errorMessage = failureMessage)
                onResult(failureMessage)
            }
        }
    }

    companion object {
        fun factory(repository: PantryRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PantryViewModel(repository) as T
                }
            }
        }
    }
}
