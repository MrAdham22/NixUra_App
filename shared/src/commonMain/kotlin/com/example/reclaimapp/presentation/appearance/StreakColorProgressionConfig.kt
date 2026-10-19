package com.example.reclaimapp.presentation.appearance

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Provisional streak thresholds for habit-card color progression (product review in Phase 3).
 * Upper bounds for interpolation segments; final stop stays at [stopCount - 1] once elapsed >= last threshold.
 */
data class StreakColorProgressionConfig(
    val segmentUpperBounds: List<Duration>,
) {
    init {
        require(segmentUpperBounds.size >= 2) { "Need at least start and one bound" }
        require(segmentUpperBounds.first() == Duration.ZERO) { "First bound must be zero" }
        require(segmentUpperBounds.zipWithNext().all { (a, b) -> a < b }) { "Bounds must be strictly increasing" }
    }

    val stopCount: Int get() = segmentUpperBounds.size

    companion object {
        /** Provisional: 0, 1d, 3d, 7d, 14d, 30d — aligns with six palette stops. */
        val Default = StreakColorProgressionConfig(
            segmentUpperBounds = listOf(
                Duration.ZERO,
                1.days,
                3.days,
                7.days,
                14.days,
                30.days,
            ),
        )
    }
}
