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

/** Light text on a habit card: a warm near-white, brighter than blush so it stays readable on rose. */
val CardInkLight = Color(0xFFFFF7F5)

/** Dark text on a habit card (the light end of the streak). */
val CardInkDark = NixUraPalette.Navy

/** Text colors on a habit card: [primary] for names and counters, [muted] for labels and secondary lines. */
data class CardInk(val primary: Color, val muted: Color)

/** The text colors that read best on this card color (see [prefersLightContentOnCard]). */
fun Color.cardInk(): CardInk =
    if (prefersLightContentOnCard()) CardInk(CardInkLight, CardInkLight.copy(alpha = 0.86f))
    else CardInk(CardInkDark, CardInkDark.copy(alpha = 0.80f))

/** WCAG relative luminance (0 = black, 1 = white) of an opaque color. */
fun Color.relativeLuminance(): Float {
    fun channel(v: Float): Double = if (v <= 0.04045f) v / 12.92 else kotlin.math.exp(2.4 * kotlin.math.ln((v + 0.055) / 1.055))
    return (0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)).toFloat()
}

/** WCAG contrast ratio (1..21) between two opaque colors. */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.relativeLuminance()
    val lb = b.relativeLuminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/**
 * True when light text reads better than dark text on this card color: whichever of [CardInkLight] and
 * [CardInkDark] has the higher contrast ratio wins. (A plain brightness cut-off put blush text on rose cards at
 * about 2.4:1.)
 */
fun Color.prefersLightContentOnCard(): Boolean =
    contrastRatio(CardInkLight, this) >= contrastRatio(CardInkDark, this)
