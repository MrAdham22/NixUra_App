package com.adhamelgendy.nixura.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.presentation.strings.Strings
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/** Wall-clock "now" that updates every [periodMillis]; drives live countdowns. */
@Composable
fun rememberNowMillis(periodMillis: Long = 1_000L): State<Long> {
    val clock = koinInject<Clock>()
    val now = remember { mutableStateOf(clock.nowEpochMillis()) }
    LaunchedEffect(periodMillis) {
        while (true) {
            now.value = clock.nowEpochMillis()
            delay(periodMillis)
        }
    }
    return now
}

fun formatClockMinutes(minutesOfDay: Int, s: Strings): String {
    val hour24 = (minutesOfDay / 60).coerceIn(0, 23)
    val minute = (minutesOfDay % 60).coerceIn(0, 59)
    val suffix = if (hour24 < 12) s.am else s.pm
    val hour12 = when (val h = hour24 % 12) {
        0 -> 12
        else -> h
    }
    return "$hour12:${minute.toString().padStart(2, '0')} $suffix"
}

/** e.g. "Tue, 20 Oct 2026 · 8:30 PM"; with [includeYear] = false (home-screen widgets): "Tue, 20 Oct · 8:30 PM". */
fun formatDateTime(epochMillis: Long, days: DayCalculator, s: Strings, includeYear: Boolean = true): String {
    val dayIndex = days.dayIndexOf(epochMillis)
    val minutes = (days.millisOfDay(epochMillis) / 60_000L).toInt()
    return "${formatDate(dayIndex, s, includeWeekday = true, includeYear = includeYear)} · ${formatClockMinutes(minutes, s)}"
}

fun formatDate(dayIndex: Long, s: Strings, includeWeekday: Boolean = false, includeYear: Boolean = true): String {
    val civil = CivilCalendar.civilFromDayIndex(dayIndex)
    val weekday = s.weekdayShort[CivilCalendar.isoWeekday(dayIndex) - 1]
    val base = if (includeYear) "${civil.day} ${s.monthShort[civil.month - 1]} ${civil.year}" else "${civil.day} ${s.monthShort[civil.month - 1]}"
    return if (includeWeekday) "$weekday, $base" else base
}

fun formatLead(leadMillis: Long, s: Strings): String {
    val minutes = leadMillis / 60_000L
    return when {
        leadMillis <= 0L -> s.atEventTime
        minutes < 60 -> s.minutesBefore(minutes.toInt())
        minutes < 60 * 24 -> s.hoursBefore((minutes / 60).toInt())
        else -> s.daysBefore((minutes / (60 * 24)).toInt())
    }
}

/** Weekday letters in NixUra's week order, Sunday first ("S M T W T F S"). */
fun com.adhamelgendy.nixura.presentation.strings.Strings.weekdayLettersSundayFirst(): List<String> =
    listOf(weekdayLetters[6]) + weekdayLetters.take(6)

/**
 * A name the user typed, for display: first letter capitalized ("smoking" → "Smoking"). Only for showing; the saved
 * name, matching and anything sent to the coach keep exactly what the user typed.
 */
fun String.forDisplay(): String = trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

/** Every word capitalized, for a person's name ("adham ahmed" → "Adham Ahmed"). */
fun String.personForDisplay(): String = trim().split(Regex("\\s+")).joinToString(" ") { it.forDisplay() }
