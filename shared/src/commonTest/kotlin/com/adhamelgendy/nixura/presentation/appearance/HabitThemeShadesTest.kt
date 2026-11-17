package com.adhamelgendy.nixura.presentation.appearance

import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.presentation.theme.DarkNixColors
import com.adhamelgendy.nixura.presentation.theme.LightNixColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HabitThemeShadesTest {

    @Test
    fun prayerOneToFiveGivesFiveDistinctSteps() {
        val levels = (0..5).map { DayIntensity.level(it / 5f) }
        assertEquals(listOf(0, 1, 2, 3, 4, 5), levels)
    }

    @Test
    fun eightCupsGroupIntoFourPartialStepsThenFull() {
        val levels = (0..8).map { DayIntensity.level(it / 8f) }
        assertEquals(listOf(0, 1, 1, 2, 2, 3, 3, 4, 5), levels)
    }

    @Test
    fun outOfRangeProgressIsClamped() {
        assertEquals(0, DayIntensity.level(-0.5f))
        assertEquals(0, DayIntensity.level(Float.NaN))
        assertEquals(DayIntensity.FULL, DayIntensity.level(1.7f))
        assertEquals(1, DayIntensity.level(0.001f))
        assertEquals(DayIntensity.PARTIAL_LEVELS, DayIntensity.level(0.999f))
    }

    @Test
    fun legendRunsFromNothingToFullAndGetsStrongerEachStep() {
        assertEquals((0..DayIntensity.FULL).toList(), DayIntensity.legendLevels)
        val fractions = DayIntensity.legendLevels.map { DayIntensity.fraction(it) }
        assertEquals(0f, fractions.first())
        assertEquals(1f, fractions.last())
        fractions.zipWithNext().forEach { (a, b) -> assertTrue(b > a, "fractions must rise: $fractions") }
    }

    @Test
    fun themesKeepTheirColourFamily() {
        listOf(DarkNixColors, LightNixColors).forEach { c ->
            val water = HabitThemes.visualFor(HabitTheme.WATER, c).accent
            val gold = HabitThemes.visualFor(HabitTheme.PRAYER, c).accent
            assertTrue(water.blue > water.red && water.blue > water.green, "water is blue (dark=${c.isDark})")
            // Warm gold, not mustard: red clearly above green, little blue.
            assertTrue(gold.red > gold.green && gold.green / gold.red < 0.8f && gold.blue < gold.green, "gold is warm (dark=${c.isDark})")
            HabitTheme.entries.filter { it != HabitTheme.WATER && it != HabitTheme.PRAYER }.forEach {
                assertEquals(c.positive, HabitThemes.visualFor(it, c).accent)
            }
        }
    }

    @Test
    fun aFullDayStandsOutFromAnEmptyDayInBothThemes() {
        listOf(DarkNixColors, LightNixColors).forEach { c ->
            listOf(HabitTheme.WATER, HabitTheme.PRAYER).forEach { theme ->
                val full = DayIntensity.shade(DayIntensity.FULL, HabitThemes.accentFor(theme, c), c.surfaceRaised)
                val ratio = contrastRatio(full, c.surfaceRaised)
                assertTrue(ratio >= 3f, "$theme full vs empty (dark=${c.isDark}): $ratio")
            }
        }
    }

    @Test
    fun dayNumbersReadOnEveryStep() {
        listOf(DarkNixColors, LightNixColors).forEach { c ->
            HabitTheme.entries.forEach { theme ->
                (1..DayIntensity.FULL).forEach { level ->
                    val accent = HabitThemes.accentFor(theme, c)
                    val square = DayIntensity.shade(level, accent, c.surfaceRaised, DayIntensity.partialFloor(accent, c.surfaceRaised, c.isDark))
                    val ratio = contrastRatio(DayIntensity.inkOn(square), square)
                    // Bold day numbers; 3.9 is about the best two inks can do on a mid-tone (see GoodFillContrastTest).
                    assertTrue(ratio >= 3.9f, "$theme level $level (dark=${c.isDark}): $ratio")
                }
            }
        }
    }

    @Test
    fun calendarStepsAreDistinctAndKeepTheAccentHue() {
        listOf(DarkNixColors, LightNixColors).forEach { c ->
            listOf(HabitTheme.WATER, HabitTheme.PRAYER).forEach { theme ->
                val accent = HabitThemes.accentFor(theme, c)
                val floor = DayIntensity.partialFloor(accent, c.surfaceRaised, c.isDark)
                val steps = DayIntensity.legendLevels.map { DayIntensity.shade(it, accent, c.surfaceRaised, floor) }
                assertEquals(steps.size, steps.toSet().size, "$theme steps (dark=${c.isDark})")
                // Every partial step keeps the accent's dominant channel (blue stays blue, gold stays red-yellow).
                steps.drop(1).forEach { sq ->
                    if (theme == HabitTheme.WATER) assertTrue(sq.blue >= sq.red, "$theme $sq") else assertTrue(sq.red >= sq.blue, "$theme $sq")
                }
            }
        }
    }
}
