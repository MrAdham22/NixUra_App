package com.example.reclaimapp.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.time.CivilCalendar
import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.presentation.strings.Strings
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

/** e.g. "Tue, 20 Oct 2026 · 8:30 PM" */
fun formatDateTime(epochMillis: Long, days: DayCalculator, s: Strings): String {
    val dayIndex = days.dayIndexOf(epochMillis)
    val minutes = (days.millisOfDay(epochMillis) / 60_000L).toInt()
    return "${formatDate(dayIndex, s, includeWeekday = true)} · ${formatClockMinutes(minutes, s)}"
}

fun formatDate(dayIndex: Long, s: Strings, includeWeekday: Boolean = false): String {
    val civil = CivilCalendar.civilFromDayIndex(dayIndex)
    val weekday = s.weekdayShort[CivilCalendar.isoWeekday(dayIndex) - 1]
    val base = "${civil.day} ${s.monthShort[civil.month - 1]} ${civil.year}"
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
