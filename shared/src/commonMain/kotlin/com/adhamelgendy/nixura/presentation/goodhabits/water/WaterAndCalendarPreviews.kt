package com.adhamelgendy.nixura.presentation.goodhabits.water

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.domain.recognition.GoodHabitFillStyle
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.theme.WaterGoal
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.presentation.appearance.HabitThemes
import com.adhamelgendy.nixura.presentation.components.GoodHabitCard
import com.adhamelgendy.nixura.presentation.components.MonthCalendar
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.goodhabits.GoodHabitUi
import com.adhamelgendy.nixura.presentation.strings.ArabicStrings
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.strings.Strings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixUraTheme

// Previews of Session 3's states: themed calendars (water, prayer) and the water card, dark + light, LTR + RTL.
// RTL previews use the Arabic strings, which also make the week start on Saturday.

private val PreviewToday = CivilCalendar.dayIndexFromCivil(2026, 11, 17)

private fun previewHabit(theme: HabitTheme, todayCount: Int): GoodHabitUi {
    val water = if (theme == HabitTheme.WATER) WaterGoal.fromLiters(2.0) else null
    val target = water?.servingsPerDay ?: 5
    // A month of varied days, so every intensity step shows.
    val counts = (0L until 17L).associate { back -> (PreviewToday - back) to ((back * 3 + 1) % (target + 2)).toInt() }
    return GoodHabitUi(
        id = 1, name = if (water != null) "Drink water" else "Pray", dailyTarget = target, colorArgb = null,
        todayCount = todayCount, doneToday = todayCount >= target, currentStreak = 3, bestStreak = 6, totalTimesDone = 40,
        daysCompleted = 6, completion7 = 0.6f, completion30 = 0.5f, countsByDay = counts + (PreviewToday to todayCount),
        createdDayIndex = PreviewToday - 40, fillStyle = theme.fillStyle, topic = theme.topic, theme = theme, water = water,
    )
}

@Composable
private fun PreviewFrame(dark: Boolean, rtl: Boolean, content: @Composable () -> Unit) {
    val strings: Strings = if (rtl) ArabicStrings else Strings()
    NixUraTheme(darkTheme = dark) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            LocalStrings provides strings,
        ) {
            Column(Modifier.background(Nix.colors.background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
        }
    }
}

@Composable
private fun CalendarSample(theme: HabitTheme) {
    val habit = previewHabit(theme, 3)
    NixCard {
        MonthCalendar(
            year = 2026, month = 11, countsByDay = habit.countsByDay, progressOf = habit::dayProgress,
            accent = HabitThemes.visualFor(theme, Nix.colors).accent, todayIndex = PreviewToday,
            createdDayIndex = habit.createdDayIndex, selectedDay = null, onSelectDay = {}, onPrevMonth = {}, onNextMonth = {},
            canGoNext = false,
        )
    }
}

@Composable
private fun WaterCardSample() {
    val habit = previewHabit(HabitTheme.WATER, 5)
    GoodHabitCard(habit, PreviewToday, onClick = {}, onPlus = {}, onMinus = {})
}

@Preview @Composable private fun WaterCalendarDarkPreview() = PreviewFrame(dark = true, rtl = false) { CalendarSample(HabitTheme.WATER) }
@Preview @Composable private fun WaterCalendarLightPreview() = PreviewFrame(dark = false, rtl = false) { CalendarSample(HabitTheme.WATER) }
@Preview @Composable private fun PrayerCalendarDarkPreview() = PreviewFrame(dark = true, rtl = false) { CalendarSample(HabitTheme.PRAYER) }
@Preview @Composable private fun PrayerCalendarLightPreview() = PreviewFrame(dark = false, rtl = false) { CalendarSample(HabitTheme.PRAYER) }
@Preview @Composable private fun PrayerCalendarDarkRtlPreview() = PreviewFrame(dark = true, rtl = true) { CalendarSample(HabitTheme.PRAYER) }
@Preview @Composable private fun WaterCalendarLightRtlPreview() = PreviewFrame(dark = false, rtl = true) { CalendarSample(HabitTheme.WATER) }
@Preview @Composable private fun PlainCalendarDarkPreview() = PreviewFrame(dark = true, rtl = false) { CalendarSample(HabitTheme.WORKOUT) }

@Preview @Composable private fun WaterCardDarkPreview() = PreviewFrame(dark = true, rtl = false) { WaterCardSample() }
@Preview @Composable private fun WaterCardLightPreview() = PreviewFrame(dark = false, rtl = false) { WaterCardSample() }
@Preview @Composable private fun WaterCardDarkRtlPreview() = PreviewFrame(dark = true, rtl = true) { WaterCardSample() }
@Preview @Composable private fun WaterCardLightRtlPreview() = PreviewFrame(dark = false, rtl = true) { WaterCardSample() }
