package com.adhamelgendy.nixura.presentation.appearance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class CardGradientTest {
    private val mapper = StreakColorMapper()
    private val colors = listOf(null, 0xFFAE445AL, 0xFFF39F5AL, 0xFF2A9D8FL, 0xFF4C6FFFL, 0xFFE9C46AL)
    private val moments: List<Duration> = (0..60).map { it.days } + (0..24).map { it.hours }

    @Test
    fun middleIsTheStreakColor() {
        colors.forEach { color ->
            moments.forEach { elapsed ->
                assertEquals(mapper.cardBackground(elapsed, color), mapper.cardGradient(elapsed, color).middle)
            }
        }
    }

    @Test
    fun everyPartOfTheCardKeepsTheSameTextColor() {
        colors.forEach { color ->
            moments.forEach { elapsed ->
                val g = mapper.cardGradient(elapsed, color)
                val light = g.middle.prefersLightContentOnCard()
                assertEquals(light, g.start.prefersLightContentOnCard(), "start at $elapsed for $color")
                assertEquals(light, g.end.prefersLightContentOnCard(), "end at $elapsed for $color")
            }
        }
    }

    @Test
    fun aNewStreakIsAlreadyAGradient() {
        colors.forEach { color ->
            val g = mapper.cardGradient(Duration.ZERO, color)
            assertNotEquals(g.middle, g.end, "flat card for $color")
        }
    }
}
