package com.example.reclaimapp.presentation.appearance

import kotlin.time.Duration

/**
 * Pure progression logic (testable without Compose).
 * Returns segment index and fraction in [0, 1] for interpolation between palette stops.
 */
data class StreakProgressionSegment(
    val fromStopIndex: Int,
    val toStopIndex: Int,
    val fraction: Float,
)

object StreakProgression {
    fun segmentFor(elapsed: Duration, config: StreakColorProgressionConfig): StreakProgressionSegment {
        require(elapsed >= Duration.ZERO) { "Elapsed must be non-negative" }
        val bounds = config.segmentUpperBounds
        val lastIndex = bounds.lastIndex

        if (elapsed >= bounds[lastIndex]) {
            return StreakProgressionSegment(
                fromStopIndex = lastIndex,
                toStopIndex = lastIndex,
                fraction = 1f,
            )
        }

        var segment = 0
        while (segment < lastIndex && elapsed >= bounds[segment + 1]) {
            segment++
        }

        val start = bounds[segment]
        val end = bounds[segment + 1]
        val rangeMillis = (end - start).inWholeMilliseconds
        val fraction = if (rangeMillis == 0L) {
            1f
        } else {
            ((elapsed - start).inWholeMilliseconds.toFloat() / rangeMillis).coerceIn(0f, 1f)
        }

        return StreakProgressionSegment(
            fromStopIndex = segment,
            toStopIndex = segment + 1,
            fraction = fraction,
        )
    }

    /**
     * Active stage index (0..stopCount-1) for non-color indicators.
     * Reflects milestones actually reached (upper bounds), not interpolation midpoint.
     */
    fun activeStageIndex(elapsed: Duration, config: StreakColorProgressionConfig): Int {
        require(elapsed >= Duration.ZERO) { "Elapsed must be non-negative" }
        val bounds = config.segmentUpperBounds
        var index = 0
        for (i in bounds.indices) {
            if (elapsed >= bounds[i]) {
                index = i
            } else {
                break
            }
        }
        return index.coerceIn(0, config.stopCount - 1)
    }
}
