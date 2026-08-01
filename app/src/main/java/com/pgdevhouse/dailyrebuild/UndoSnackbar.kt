package com.pgdevhouse.dailyrebuild

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

/**
 * Shows one consistent destructive-action snackbar throughout Daily Rebuild.
 * The delete is performed before this function is called. When the user taps
 * Undo, the supplied restore block must put the complete original record back.
 */
suspend fun SnackbarHostState.showUndoableDelete(
    message: String,
    restoredMessage: String,
    restoreFailedMessage: String = "Could not restore that item.",
    onUndo: suspend () -> Unit
) {
    val result = showSnackbar(
        message = message,
        actionLabel = "Undo",
        withDismissAction = true,
        duration = SnackbarDuration.Long
    )

    if (result != SnackbarResult.ActionPerformed) return

    try {
        onUndo()
        showSnackbar(message = restoredMessage)
    } catch (_: Exception) {
        showSnackbar(message = restoreFailedMessage)
    }
}
