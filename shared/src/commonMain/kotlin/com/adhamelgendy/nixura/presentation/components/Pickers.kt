package com.adhamelgendy.nixura.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import com.adhamelgendy.nixura.presentation.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import org.koin.compose.koinInject

/**
 * Hour and minute wheels (every minute, both wrap around) with AM / PM. [minutesOfDay] is 0 until 1440.
 * [visibleRows] is 5 when the picker is alone in its dialog, 3 under a calendar.
 */
@Composable
fun TimeOfDayPicker(minutesOfDay: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, visibleRows: Int = 5) {
    val s = LocalStrings.current
    val time = ClockSelection.of(minutesOfDay)
    fun emit(next: ClockSelection) = onChange(next.minutesOfDay)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // A clock reads hour:minute from the left in Arabic too.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberWheel(time.hour12, 1..12, { emit(time.copy(hour12 = it)) }, s.pickerHour, loop = true, visibleRows = visibleRows)
                Text(":", style = NixType.Heading, color = Nix.colors.textMuted)
                NumberWheel(
                    time.minute, 0..59, { emit(time.copy(minute = it)) }, s.pickerMinute,
                    loop = true, visibleRows = visibleRows, format = { it.toString().padStart(2, '0') },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NixChip(s.am, selected = !time.pm, onClick = { emit(time.copy(pm = false)) })
            NixChip(s.pm, selected = time.pm, onClick = { emit(time.copy(pm = true)) })
        }
    }
}

@Composable
fun TimePickerDialog(
    initialMinutes: Int,
    title: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    var minutes by remember { mutableStateOf(initialMinutes) }
    Dialog(onDismissRequest = onDismiss) {
        NixCard(Modifier.verticalScroll(rememberScrollState()), padding = PaddingValues(22.dp)) {
            Text(title, style = NixType.Heading, color = Nix.colors.textPrimary)
            Spacer(Modifier.height(16.dp))
            TimeOfDayPicker(minutes, { minutes = it })
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NixButton(s.cancel, onDismiss, Modifier.weight(1f), style = NixButtonStyle.Secondary)
                NixButton(s.done, { onConfirm(minutes) }, Modifier.weight(1f))
            }
        }
    }
}

/** Date grid + time selector. Returns UTC epoch millis of the chosen local date and time. */
@Composable
fun DateTimePickerDialog(
    initialEpochMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    val c = Nix.colors
    val days = koinInject<DayCalculator>()
    val initialDay = remember { days.dayIndexOf(initialEpochMillis) }
    val initialCivil = remember { CivilCalendar.civilFromDayIndex(initialDay) }
    var monthIndex by remember { mutableStateOf(initialCivil.year * 12 + initialCivil.month - 1) }
    var selectedDay by remember { mutableStateOf(initialDay) }
    var minutes by remember { mutableStateOf((days.millisOfDay(initialEpochMillis) / 60_000L).toInt()) }
    val todayIndex = remember { days.dayIndexOf(initialEpochMillis) }

    val year = monthIndex.floorDiv(12)
    val month = monthIndex.mod(12) + 1
    val firstDay = CivilCalendar.dayIndexFromCivil(year, month, 1)
    val lead = CivilCalendar.weekColumn(firstDay)
    val length = CivilCalendar.monthLength(year, month)
    val rows = (lead + length + 6) / 7

    Dialog(onDismissRequest = onDismiss) {
        NixCard(Modifier.verticalScroll(rememberScrollState()), padding = PaddingValues(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.monthYear(s.monthNames[month - 1], year), style = NixType.Heading, color = c.textPrimary, modifier = Modifier.weight(1f))
                NixIconButton(NixIcon.Back, { monthIndex -= 1 }, size = 34.dp)
                Spacer(Modifier.width(8.dp))
                NixIconButton(NixIcon.Chevron, { monthIndex += 1 }, size = 34.dp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                CivilCalendar.inWeekOrder(s.weekdayLetters).forEach {
                    Text(it, style = NixType.Small, color = c.textMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(6.dp))
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val dom = row * 7 + col - lead + 1
                        if (dom < 1 || dom > length) {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val day = firstDay + dom - 1
                            val selected = day == selectedDay
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) c.accent else Color.Transparent)
                                    .border(1.dp, if (day == todayIndex && !selected) c.textSecondary else Color.Transparent, CircleShape)
                                    .clickable { selectedDay = day },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(dom.toString(), style = NixType.SmallStrong, color = if (selected) c.onAccent else c.textPrimary)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            TimeOfDayPicker(minutes, { minutes = it }, visibleRows = 3)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NixButton(s.cancel, onDismiss, Modifier.weight(1f), style = NixButtonStyle.Secondary)
                NixButton(s.done, {
                    val civil = CivilCalendar.civilFromDayIndex(selectedDay)
                    onConfirm(days.epochMillisOf(civil.year, civil.month, civil.day, minutes / 60, minutes % 60))
                }, Modifier.weight(1f))
            }
        }
    }
}
