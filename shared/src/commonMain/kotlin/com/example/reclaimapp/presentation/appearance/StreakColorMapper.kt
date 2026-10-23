package com.example.reclaimapp.presentation.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.reclaimapp.presentation.theme.NixUraPalette
import kotlin.time.Duration

class StreakColorMapper(
    private val config: StreakColorProgressionConfig = StreakColorProgressionConfig.Default,
) {
    private val stops: List<Color> = NixUraPalette.streakStops

    fun cardBackgroundForActiveStreak(elapsed: Duration): Color {
        val segment = StreakProgression.segmentFor(elapsed, config)
        val from = stops[segment.fromStopIndex.coerceIn(stops.indices)]
        val to = stops[segment.toStopIndex.coerceIn(stops.indices)]
        return if (segment.fromStopIndex == segment.toStopIndex) {
            from
        } else {
            lerp(from, to, segment.fraction)
        }
    }

    /**
     * Card color for a habit. With no user-chosen color this is the default NixUra brand progression
     * ([cardBackgroundForActiveStreak]); with a chosen color the card starts at an almost-darkest shade of
     * it and gets lighter day after day (see [ShadeRamp]).
     */
    fun cardBackground(elapsed: Duration, customBaseArgb: Long?): Color {
        if (customBaseArgb == null) return cardBackgroundForActiveStreak(elapsed)
        val ramp = ShadeRamp.stops(customBaseArgb, config.stopCount)
        val argb = ShadeRamp.colorFor(ramp, StreakProgression.segmentFor(elapsed, config))
        return Color(argb.toInt())
    }

    fun stageIndexFor(elapsed: Duration): Int =
        StreakProgression.activeStageIndex(elapsed, config)

    fun configLabel(): String =
        config.segmentUpperBounds.drop(1).joinToString(" → ") { formatThreshold(it) }

    private fun formatThreshold(d: Duration): String {
        val days = d.inWholeDays
        return if (days >= 1) "${days}d" else "${d.inWholeHours}h"
    }
}

/** Relative luminance heuristic for on-card text. */
fun Color.prefersLightContentOnCard(): Boolean {
    val r = red
    val g = green
    val b = blue
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    return luminance < 0.55f
}
