package com.pgdevhouse.dailyrebuild.domain

import kotlin.math.max

/**
 * Records the highest value seen so far today for one pain area.
 * Daily Rebuild applies this independently to back pain and shin-splint pain.
 */
fun recordDailyHighestPain(
    currentHighest: Float,
    candidate: Float
): Float {
    return max(
        currentHighest.coerceIn(0f, 10f),
        candidate.coerceIn(0f, 10f)
    )
}
