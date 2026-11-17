package com.adhamelgendy.nixura.presentation.locale

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.domain.model.DigitStyle
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.theme.WaterGoal
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.presentation.appearance.HabitThemes
import com.adhamelgendy.nixura.presentation.components.EventCard
import com.adhamelgendy.nixura.presentation.components.GoodHabitCard
import com.adhamelgendy.nixura.presentation.components.MonthCalendar
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.QuitHabitRow
import com.adhamelgendy.nixura.presentation.components.SectionLabel
import com.adhamelgendy.nixura.presentation.components.Text
import com.adhamelgendy.nixura.presentation.goodhabits.GoodHabitUi
import com.adhamelgendy.nixura.presentation.goodhabits.water.dayProgress
import com.adhamelgendy.nixura.presentation.habits.HabitCardUiModel
import com.adhamelgendy.nixura.presentation.screen.StatTile
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.strings.stringsFor
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.theme.NixUraTheme
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

// Previews of the main screens' building blocks in Arabic (RTL), dark and light, plus English for comparison.
// The screens themselves read their ViewModels from Koin, so they are previewed through the parts they are made of:
// Home (greeting, quit row, water card, event), the Build card, the detail stats and the Saturday-first calendar.

private val Today = CivilCalendar.dayIndexFromCivil(2026, 11, 17)
private const val NOW = 1_795_000_000_000L

/** The app root's locale setup (strings, direction, digits, Arabic font) for previews. */
@Composable
fun LocalePreview(language: String, dark: Boolean, digits: DigitStyle = DigitStyle.WESTERN, content: @Composable () -> Unit) {
    val locale = AppLocale(language = language, digitStyle = digits, arabicFont = rememberArabicFontFamily())
    CompositionLocalProvider(
        LocalAppLocale provides locale,
        LocalLayoutDirection provides if (locale.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalStrings provides stringsFor(language),
    ) {
        NixUraTheme(darkTheme = dark) {
            Column(Modifier.background(Nix.colors.background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                content()
            }
        }
    }
}

private fun quitHabit(name: String) = HabitCardUiModel(
    habitId = 1, name = name, activeElapsed = 3.days + 5.hours, longestStreakLabel = "", activeStageIndex = 2, stageCount = 6,
    longestStreak = 9.days,
)

private fun waterHabit(name: String): GoodHabitUi {
    val water = WaterGoal.fromLiters(2.0)
    val counts = (0L until 10L).associate { back -> (Today - back) to ((back * 3 + 1) % (water.servingsPerDay + 2)).toInt() }
    return GoodHabitUi(
        id = 2, name = name, dailyTarget = water.servingsPerDay, colorArgb = null, todayCount = 5, doneToday = false,
        currentStreak = 3, bestStreak = 6, totalTimesDone = 40, daysCompleted = 6, completion7 = 0.6f, completion30 = 0.5f,
        countsByDay = counts + (Today to 5), createdDayIndex = Today - 40, fillStyle = HabitTheme.WATER.fillStyle,
        topic = HabitTheme.WATER.topic, theme = HabitTheme.WATER, water = water,
    )
}

@Composable
private fun HomeSample() {
    val s = LocalStrings.current
    val arabic = LocalAppLocale.current.isArabic
    Text(s.greetingEvening, style = NixType.Title, color = Nix.colors.textPrimary)
    SectionLabel(s.quittingSection)
    QuitHabitRow(quitHabit(if (arabic) "التدخين" else "Smoking"), onClick = {}, onCraving = {})
    SectionLabel(s.buildingSection)
    GoodHabitCard(waterHabit(if (arabic) "اشرب الماء" else "Drink water"), Today, onClick = {}, onPlus = {}, onMinus = {})
    SectionLabel(s.nextEventSection)
    val event = Event(3, if (arabic) "رحلة إلى سيوة" else "Trip to Siwa", EventCategory.TRAVEL, NOW + 4.days.inWholeMilliseconds, emptyList(), NOW)
    EventCard(event, NOW, dateText = s.dateText(s.weekdayShort[5], 21, s.monthShort[10], 2026), onClick = {})
}

@Composable
private fun DetailSample() {
    val s = LocalStrings.current
    val habit = waterHabit("")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(s.statCurrentStreak, s.streakDays(3), Modifier.weight(1f))
        StatTile(s.statBestStreak, s.daysCount(11), Modifier.weight(1f))
    }
    NixCard {
        MonthCalendar(
            year = 2026, month = 11, countsByDay = habit.countsByDay, progressOf = habit::dayProgress,
            accent = HabitThemes.visualFor(HabitTheme.WATER, Nix.colors).accent, todayIndex = Today,
            createdDayIndex = habit.createdDayIndex, selectedDay = null, onSelectDay = {}, onPrevMonth = {}, onNextMonth = {},
            canGoNext = false,
        )
    }
}

@Preview @Composable private fun HomeArabicDarkPreview() = LocalePreview("ar", dark = true) { HomeSample() }
@Preview @Composable private fun HomeArabicLightPreview() = LocalePreview("ar", dark = false) { HomeSample() }
@Preview @Composable private fun HomeArabicIndicDigitsPreview() = LocalePreview("ar", dark = true, digits = DigitStyle.ARABIC_INDIC) { HomeSample() }
@Preview @Composable private fun HomeEnglishDarkPreview() = LocalePreview("en", dark = true) { HomeSample() }
@Preview @Composable private fun DetailArabicDarkPreview() = LocalePreview("ar", dark = true) { DetailSample() }
@Preview @Composable private fun DetailArabicLightPreview() = LocalePreview("ar", dark = false) { DetailSample() }
@Preview @Composable private fun DetailEnglishLightPreview() = LocalePreview("en", dark = false) { DetailSample() }
