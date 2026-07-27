package com.pgdevhouse.dailyrebuild.domain

import kotlin.math.max

/** Business rule for the app's single daily pain value. */
fun recordDailyHighestPain(
    currentHighest: Float,
    candidate: Float
): Float {
    return max(
        currentHighest.coerceIn(0f, 10f),
        candidate.coerceIn(0f, 10f)
    )
}
