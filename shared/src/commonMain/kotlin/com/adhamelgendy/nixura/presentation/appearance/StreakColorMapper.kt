package com.adhamelgendy.nixura.presentation.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.adhamelgendy.nixura.presentation.theme.NixUraPalette
import kotlin.time.Duration

/** Three colors of a habit card, top-left to bottom-right. On-card text follows [middle]. */
data class CardGradient(val start: Color, val middle: Color, val end: Color)

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

    /**
     * The card as a color scheme instead of one flat color: it runs from where the streak has been, through
     * [cardBackground], toward the shade it is growing into. All three stay readable with the same text color.
     */
    fun cardGradient(elapsed: Duration, customBaseArgb: Long?): CardGradient {
        val colors = if (customBaseArgb == null) {
            stops
        } else {
            ShadeRamp.stops(customBaseArgb, config.stopCount).map { Color(it.toInt()) }
        }
        val segment = StreakProgression.segmentFor(elapsed, config)
        val middle = cardBackground(elapsed, customBaseArgb)
        val behind = colors[(segment.fromStopIndex - 1).coerceIn(colors.indices)]
        val ahead = colors[(segment.toStopIndex + 1).coerceIn(colors.indices)]
        return CardGradient(mixKeepingText(middle, behind, 0.55f), middle, mixKeepingText(middle, ahead, 0.60f))
    }

    /** Moves [base] toward [target], but never so far that the on-card text color would have to change. */
    private fun mixKeepingText(base: Color, target: Color, amount: Float): Color {
        var t = amount
        while (t > 0f) {
            val mixed = lerp(base, target, t)
            if (mixed.prefersLightContentOnCard() == base.prefersLightContentOnCard()) return mixed
            t -= 0.1f
        }
        return base
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
