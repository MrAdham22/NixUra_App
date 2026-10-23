package com.example.reclaimapp.presentation.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.reclaimapp.domain.time.CivilCalendar
import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import org.koin.compose.koinInject

/** Hour / minute / AM-PM selector. [minutesOfDay] is 0 until 1440. */
@Composable
fun TimeOfDayPicker(minutesOfDay: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val hour24 = (minutesOfDay / 60).coerceIn(0, 23)
    val minute = (minutesOfDay % 60).coerceIn(0, 59)
    val isPm = hour24 >= 12
    val hour12 = (hour24 % 12).let { if (it == 0) 12 else it }

    fun emit(h12: Int, m: Int, pm: Boolean) {
        val h24 = (h12 % 12) + if (pm) 12 else 0
        onChange(h24 * 60 + m)
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Stepper(hour12, { emit(it, minute, isPm) }, min = 1, max = 12, valueWidth = 44.dp)
            Text(":", style = NixType.Heading, color = Nix.colors.textMuted)
            Stepper(minute, { emit(hour12, it, isPm) }, min = 0, max = 59, step = 5, valueWidth = 44.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NixChip(s.am, selected = !isPm, onClick = { emit(hour12, minute, false) })
            NixChip(s.pm, selected = isPm, onClick = { emit(hour12, minute, true) })
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
        NixCard(padding = PaddingValues(22.dp)) {
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
    val lead = CivilCalendar.isoWeekday(firstDay) - 1
    val length = CivilCalendar.monthLength(year, month)
    val rows = (lead + length + 6) / 7

    Dialog(onDismissRequest = onDismiss) {
        NixCard(padding = PaddingValues(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${s.monthNames[month - 1]} $year", style = NixType.Heading, color = c.textPrimary, modifier = Modifier.weight(1f))
                NixIconButton(NixIcon.Back, { monthIndex -= 1 }, size = 34.dp)
                Spacer(Modifier.width(8.dp))
                NixIconButton(NixIcon.Chevron, { monthIndex += 1 }, size = 34.dp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                s.weekdayLetters.forEach {
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
            TimeOfDayPicker(minutes, { minutes = it })
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
