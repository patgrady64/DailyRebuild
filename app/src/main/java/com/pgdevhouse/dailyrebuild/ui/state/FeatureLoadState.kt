package com.pgdevhouse.dailyrebuild.ui.state

import android.util.Log

data class FeatureLoadFailure(
    val feature: String,
    val userMessage: String,
    val throwable: Exception
)

/**
 * Runs one independent feature load. A Room or service failure is logged with
 * its real exception and returned to the caller without cancelling other loads.
 */
suspend inline fun <T> captureFeatureLoad(
    feature: String,
    userMessage: String,
    crossinline block: suspend () -> T
): Pair<T?, FeatureLoadFailure?> {
    return try {
        block() to null
    } catch (throwable: Exception) {
        Log.e("DailyRebuildLoad", "$feature failed", throwable)
        null to FeatureLoadFailure(feature, userMessage, throwable)
    }
}

fun List<FeatureLoadFailure>.summaryMessage(): String? {
    if (isEmpty()) return null
    if (size == 1) return first().userMessage
    return first().userMessage + " ${size - 1} other section(s) also could not refresh."
}
