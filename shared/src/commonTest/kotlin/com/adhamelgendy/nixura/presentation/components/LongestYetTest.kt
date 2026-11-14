package com.adhamelgendy.nixura.presentation.components

import com.adhamelgendy.nixura.presentation.habits.HabitCardUiModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class LongestYetTest {
    private fun card(active: Duration, longest: Duration) =
        HabitCardUiModel(1, "Smoking", active, "", 0, 6, longestStreak = longest)

    @Test
    fun theRunningStreakIsTheRecord() = assertTrue(card(3.days, 3.days).isLongestYet())

    @Test
    fun anOlderStreakWasLonger() = assertFalse(card(2.hours, 5.days).isLongestYet())

    @Test
    fun aStreakThatJustStartedIsNotARecord() = assertFalse(card(Duration.ZERO, Duration.ZERO).isLongestYet())
}
