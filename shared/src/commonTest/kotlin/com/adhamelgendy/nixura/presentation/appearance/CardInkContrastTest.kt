package com.adhamelgendy.nixura.presentation.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class CardInkContrastTest {
    private val mapper = StreakColorMapper()
    private val colors = listOf(null, 0xFFAE445AL, 0xFF862549L, 0xFFF39F5AL, 0xFF8E44ADL, 0xFF2A9D8FL, 0xFF4C6FFFL, 0xFF3FA66BL, 0xFFE9C46AL)
    private val moments: List<Duration> = (0..60).map { it.days } + (0..24).map { it.hours }

    private fun worst(pick: (CardInk) -> Color, part: (CardGradient) -> Color): Pair<Float, String> {
        var min = Float.MAX_VALUE
        var where = ""
        colors.forEach { color ->
            moments.forEach { elapsed ->
                val g = mapper.cardGradient(elapsed, color)
                val bg = part(g)
                val ratio = contrastRatio(pick(g.middle.cardInk()).compositeOver(bg), bg)
                if (ratio < min) { min = ratio; where = "$elapsed for $color" }
            }
        }
        return min to where
    }

    /**
     * About 4:1 is the best two inks can do on a mid-tone card (light and dark ink tie there); on-card primary text
     * is bold and 17sp or larger, so it counts as large text (3:1 needed).
     */
    @Test
    fun namesAndCountersReadOnTheCardColor() {
        val (ratio, where) = worst({ it.primary }) { it.middle }
        assertTrue(ratio >= 3.9f, "primary text $ratio:1 at $where")
    }

    @Test
    fun countersStayReadableAcrossTheWholeGradient() {
        listOf<(CardGradient) -> Color>({ it.start }, { it.end }).forEach { part ->
            val (ratio, where) = worst({ it.primary }, part)
            assertTrue(ratio >= 3.0f, "primary text $ratio:1 at $where")
        }
    }

    @Test
    fun labelsReadOnTheCardColor() {
        val (ratio, where) = worst({ it.muted }) { it.middle }
        assertTrue(ratio >= 3.0f, "muted text $ratio:1 at $where")
    }

    @Test
    fun theOldBrightnessCutOffCaseNowPicksTheBetterInk() {
        val rose = Color(0xFFAE445A)
        assertTrue(contrastRatio(rose.cardInk().primary, rose) >= 4.5f)
    }
}
