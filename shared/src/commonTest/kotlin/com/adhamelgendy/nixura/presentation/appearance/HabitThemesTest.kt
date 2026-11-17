package com.adhamelgendy.nixura.presentation.appearance

import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.presentation.theme.DarkNixColors
import com.adhamelgendy.nixura.presentation.theme.LightNixColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HabitThemesTest {

    @Test
    fun waterIsBluePrayerIsGoldTheRestKeepTheGreen() {
        listOf(DarkNixColors, LightNixColors).forEach { c ->
            assertEquals(c.water, HabitThemes.visualFor(HabitTheme.WATER, c).accent)
            assertNotEquals(c.positive, HabitThemes.visualFor(HabitTheme.PRAYER, c).accent)
            listOf(HabitTheme.WALK, HabitTheme.WORKOUT, HabitTheme.READING, HabitTheme.SLEEP, HabitTheme.DEFAULT)
                .forEach { assertEquals(c.positive, HabitThemes.visualFor(it, c).accent, it.name) }
        }
    }

    @Test
    fun everyThemeButDefaultHasItsOwnAnimation() {
        val kinds = HabitTheme.entries.map { HabitThemes.visualFor(it, DarkNixColors).animation }
        assertEquals(HabitTheme.entries.size, kinds.toSet().size)
        assertEquals(HabitThemeAnimationKind.NONE, HabitThemes.visualFor(HabitTheme.DEFAULT, DarkNixColors).animation)
    }
}
