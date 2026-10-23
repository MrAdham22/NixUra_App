package com.example.reclaimapp.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reclaimapp.domain.calculator.CounterParts
import com.example.reclaimapp.domain.calculator.CounterUnit
import com.example.reclaimapp.domain.calculator.DayStatus
import com.example.reclaimapp.domain.calculator.GoodHabitStatsCalculator
import com.example.reclaimapp.domain.calculator.toCounterParts
import com.example.reclaimapp.domain.coach.CoachEngine
import com.example.reclaimapp.domain.model.Event
import com.example.reclaimapp.domain.time.CivilCalendar
import com.example.reclaimapp.presentation.appearance.ShadeRamp
import com.example.reclaimapp.presentation.appearance.StreakColorMapper
import com.example.reclaimapp.presentation.appearance.StreakColorProgressionConfig
import com.example.reclaimapp.presentation.appearance.prefersLightContentOnCard
import com.example.reclaimapp.presentation.goodhabits.GoodHabitUi
import com.example.reclaimapp.presentation.habits.HabitCardUiModel
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.strings.Strings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import com.example.reclaimapp.presentation.theme.NixUraPalette
import com.example.reclaimapp.presentation.util.formatElapsed
import org.koin.compose.koinInject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val BadGlyphs = listOf(NixIcon.Flame, NixIcon.Shield, NixIcon.Moon, NixIcon.Sparkle, NixIcon.Leaf, NixIcon.Sun)
private val GoodGlyphs = listOf(NixIcon.Leaf, NixIcon.Sun, NixIcon.Sparkle, NixIcon.CircleCheck, NixIcon.Moon, NixIcon.Clock)

fun badGlyphFor(id: Long): NixIcon = BadGlyphs[(id.coerceAtLeast(0) % BadGlyphs.size).toInt()]
fun goodGlyphFor(id: Long): NixIcon = GoodGlyphs[(id.coerceAtLeast(0) % GoodGlyphs.size).toInt()]

/** Preset card colors. `null` (Auto) uses the default NixUra brand progression. */
val HabitColorPresets: List<Long> = listOf(
    0xFFAE445A, 0xFF862549, 0xFFF39F5A, 0xFF8E44AD, 0xFF2A9D8F, 0xFF4C6FFF, 0xFF3FA66B, 0xFFE9C46A,
)

fun unitLabel(unit: CounterUnit, s: Strings): String = when (unit) {
    CounterUnit.DAYS -> s.unitDays
    CounterUnit.HOURS -> s.unitHours
    CounterUnit.MINUTES -> s.unitMinutes
    CounterUnit.SECONDS -> s.unitSeconds
}

/** Days → hours → minutes → seconds blocks. Shows from the largest non-zero unit down to seconds. */
@Composable
fun CounterBlocks(
    parts: CounterParts,
    color: Color,
    mutedColor: Color,
    valueSize: Int,
    modifier: Modifier = Modifier,
    gap: Dp = 16.dp,
) {
    val s = LocalStrings.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Bottom) {
        parts.visibleUnits.forEach { unit ->
            Column {
                val value = parts.valueOf(unit)
                Text(
                    text = if (unit == CounterUnit.DAYS) value.toString() else value.toString().padStart(2, '0'),
                    style = NixType.counter(valueSize),
                    color = color,
                    maxLines = 1,
                )
                Text(unitLabel(unit, s).uppercase(), style = NixType.Eyebrow.copy(fontSize = 9.sp), color = mutedColor, maxLines = 1)
            }
        }
    }
}

@Composable
fun GlyphBadge(icon: NixIcon, tint: Color, background: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(background),
        contentAlignment = Alignment.Center,
    ) { NixIconView(icon, tint, size = size * 0.55f) }
}

@Composable
fun rememberStreakMapper(): StreakColorMapper = remember { StreakColorMapper() }

/** Live card color for a bad habit: default brand progression or the user's chosen color ramp. */
@Composable
fun habitCardColor(model: HabitCardUiModel): Color {
    val mapper = rememberStreakMapper()
    val target = mapper.cardBackground(model.activeElapsed, model.colorArgb)
    val animated by animateColorAsState(target, tween(900))
    return animated
}

@Composable
fun BadHabitCard(
    model: HabitCardUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val engine = koinInject<CoachEngine>()
    val background = habitCardColor(model)
    val lightOnCard = background.prefersLightContentOnCard()
    val primary = if (lightOnCard) NixUraPalette.onDarkPrimary else NixUraPalette.onLightPrimary
    val muted = if (lightOnCard) NixUraPalette.onDarkMuted else NixUraPalette.onLightMuted
    val shape = RoundedCornerShape(26.dp)
    val sheen = if (lightOnCard) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f)
    val category = remember(model.name) { engine.categoryFor(model.name) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(background, lerp(background, sheen.copy(alpha = 1f), 0.10f))), shape)
            .border(1.dp, if (lightOnCard) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f), shape)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphBadge(badGlyphFor(model.habitId), primary, primary.copy(alpha = 0.14f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(model.name, style = NixType.Heading, color = primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(category.label, style = NixType.Small, color = muted, maxLines = 1)
            }
            NixIconView(NixIcon.Chevron, muted, size = 20.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s.clearFor.uppercase(), style = NixType.Eyebrow, color = muted)
            CounterBlocks(model.activeElapsed.toCounterParts(), primary, muted, valueSize = 30)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressStageIndicator(
                stageCount = model.stageCount,
                activeStageIndex = model.activeStageIndex,
                labelColor = muted,
                activeStageColor = primary,
                inactiveStageColor = muted.copy(alpha = 0.45f),
                modifier = Modifier.weight(1f),
            )
        }
        Text(s.longestLabel(model.longestStreakLabel), style = NixType.Small, color = muted)
    }
}

// ---------- Good habits: day squares, week strip, month calendar ----------

@Composable
fun DaySquare(
    status: DayStatus,
    modifier: Modifier = Modifier,
    label: String? = null,
    isToday: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val c = Nix.colors
    val shape = RoundedCornerShape(9.dp)
    val bg = when {
        !enabled -> Color.Transparent
        status == DayStatus.DONE -> c.positive
        status == DayStatus.PARTIAL -> c.positiveSoft
        else -> c.surfaceRaised
    }
    val borderColor = when {
        selected -> c.accent
        isToday -> c.textSecondary
        status == DayStatus.PARTIAL && enabled -> c.positive.copy(alpha = 0.55f)
        !enabled -> c.border.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    val textColor = when {
        !enabled -> c.textMuted.copy(alpha = 0.5f)
        status == DayStatus.DONE -> Color(0xFF0E2A1D)
        else -> c.textMuted
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(bg, shape)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) Text(label, style = NixType.SmallStrong, color = textColor, textAlign = TextAlign.Center)
    }
}

/** Last 7 days, oldest first, ending today. */
@Composable
fun WeekStrip(
    countsByDay: Map<Long, Int>,
    target: Int,
    todayIndex: Long,
    createdDayIndex: Long,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (offset in 6 downTo 0) {
            val day = todayIndex - offset
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(s.weekdayLetters[CivilCalendar.isoWeekday(day) - 1], style = NixType.Small, color = Nix.colors.textMuted)
                DaySquare(
                    status = GoodHabitStatsCalculator.statusOf(countsByDay[day] ?: 0, target),
                    modifier = Modifier.fillMaxWidth(),
                    isToday = offset == 0,
                    enabled = day >= createdDayIndex - 6,
                )
            }
        }
    }
}

@Composable
fun MonthCalendar(
    year: Int,
    month: Int,
    countsByDay: Map<Long, Int>,
    target: Int,
    todayIndex: Long,
    createdDayIndex: Long,
    selectedDay: Long?,
    onSelectDay: (Long) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canGoNext: Boolean,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val c = Nix.colors
    val firstDay = CivilCalendar.dayIndexFromCivil(year, month, 1)
    val lead = CivilCalendar.isoWeekday(firstDay) - 1
    val length = CivilCalendar.monthLength(year, month)
    val rows = (lead + length + 6) / 7

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${s.monthNames[month - 1]} $year", style = NixType.Heading, color = c.textPrimary, modifier = Modifier.weight(1f))
            NixIconButton(NixIcon.Back, onPrevMonth, size = 36.dp)
            Spacer(Modifier.width(8.dp))
            NixIconButton(NixIcon.Chevron, { if (canGoNext) onNextMonth() }, size = 36.dp, tint = if (canGoNext) c.textPrimary else c.textMuted.copy(alpha = 0.4f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            s.weekdayLetters.forEach {
                Text(it, style = NixType.Small, color = c.textMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 7) {
                    val dayOfMonth = row * 7 + col - lead + 1
                    if (dayOfMonth < 1 || dayOfMonth > length) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val day = firstDay + dayOfMonth - 1
                        val selectable = day <= todayIndex
                        DaySquare(
                            status = GoodHabitStatsCalculator.statusOf(countsByDay[day] ?: 0, target),
                            modifier = Modifier.weight(1f),
                            label = dayOfMonth.toString(),
                            isToday = day == todayIndex,
                            selected = day == selectedDay,
                            enabled = selectable,
                            onClick = { onSelectDay(day) },
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendDot(c.positive, s.legendDone)
            LegendDot(c.positiveSoft, s.legendPartial)
            LegendDot(c.surfaceRaised, s.legendMissed)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color).border(1.dp, Nix.colors.border, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, style = NixType.Small, color = Nix.colors.textMuted)
    }
}

@Composable
fun GoodHabitCard(
    habit: GoodHabitUi,
    todayIndex: Long,
    onClick: () -> Unit,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val c = Nix.colors
    NixCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphBadge(goodGlyphFor(habit.id), if (habit.doneToday) c.positive else c.accent, (if (habit.doneToday) c.positive else c.accent).copy(alpha = 0.14f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = NixType.Heading, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.todayProgress(habit.todayCount, habit.dailyTarget), style = NixType.Small, color = if (habit.doneToday) c.positive else c.textSecondary)
            }
            NixIconButton(NixIcon.Minus, onMinus, size = 38.dp, tint = c.textSecondary)
            Spacer(Modifier.width(8.dp))
            NixIconButton(
                if (habit.doneToday && habit.dailyTarget == 1) NixIcon.Tick else NixIcon.Plus,
                onPlus,
                size = 44.dp,
                tint = c.onAccent,
                background = if (habit.doneToday) c.positive else c.accent,
                bordered = false,
            )
        }
        Spacer(Modifier.height(14.dp))
        NixProgressBar(
            progress = habit.todayCount.toFloat() / habit.dailyTarget.coerceAtLeast(1),
            color = if (habit.doneToday) c.positive else c.accent,
            height = 6.dp,
        )
        Spacer(Modifier.height(14.dp))
        WeekStrip(habit.countsByDay, habit.dailyTarget, todayIndex, habit.createdDayIndex)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            NixIconView(NixIcon.Flame, c.accentWarm, size = 16.dp)
            Spacer(Modifier.width(6.dp))
            Text(s.streakDays(habit.currentStreak), style = NixType.SmallStrong, color = c.textSecondary)
            Spacer(Modifier.weight(1f))
            Text(s.timesDoneTotal(habit.totalTimesDone), style = NixType.Small, color = c.textMuted)
        }
    }
}

// ---------- Events ----------

@Composable
fun EventCard(
    event: Event,
    nowMillis: Long,
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val c = Nix.colors
    val remaining = (event.targetAtEpochMillis - nowMillis).milliseconds
    val passed = remaining < Duration.ZERO
    val parts = (if (passed) -remaining else remaining).toCounterParts()
    NixCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.categoryLabel(event.category).uppercase(), style = NixType.Eyebrow, color = if (passed) c.textMuted else c.accent)
                Spacer(Modifier.height(4.dp))
                Text(event.title, style = NixType.Heading, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(dateText, style = NixType.Small, color = c.textMuted)
            }
            if (event.reminderLeadMillis.isNotEmpty()) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(c.surfaceRaised).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NixIconView(NixIcon.Bell, c.accentWarm, size = 14.dp)
                    Spacer(Modifier.width(5.dp))
                    Text(event.reminderLeadMillis.size.toString(), style = NixType.SmallStrong, color = c.textSecondary)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            (if (passed) s.passedSince else s.startsIn).uppercase(),
            style = NixType.Eyebrow,
            color = c.textMuted,
        )
        Spacer(Modifier.height(6.dp))
        CounterBlocks(parts, if (passed) c.textSecondary else c.textPrimary, c.textMuted, valueSize = 26)
    }
}

@Composable
fun MomentumBars(bars: List<Float>, modifier: Modifier = Modifier) {
    val c = Nix.colors
    Row(modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        bars.forEachIndexed { index, value ->
            val isLast = index == bars.lastIndex
            Box(
                Modifier
                    .weight(1f)
                    .height((8 + 48 * value.coerceIn(0f, 1f)).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isLast) c.accent else c.accent.copy(alpha = 0.35f)),
            )
        }
    }
}

@Composable
fun ColorPickerRow(
    selected: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val c = Nix.colors
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Auto = default NixUra brand progression
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(NixUraPalette.streakStops))
                    .border(if (selected == null) 3.dp else 1.dp, if (selected == null) c.textPrimary else c.border, CircleShape)
                    .clickable { onSelect(null) },
            )
            Spacer(Modifier.height(4.dp))
            Text(s.colorAuto, style = NixType.Small, color = if (selected == null) c.textPrimary else c.textMuted)
        }
        HabitColorPresets.forEach { argb ->
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(argb.toInt()))
                    .border(if (selected == argb) 3.dp else 1.dp, if (selected == argb) c.textPrimary else c.border, CircleShape)
                    .clickable { onSelect(argb) },
            )
        }
    }
}

/** Shows how the card shade will change as the streak grows (day 0 → 30). */
@Composable
fun ShadePreview(colorArgb: Long?, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val c = Nix.colors
    val config = StreakColorProgressionConfig.Default
    val stops: List<Color> = if (colorArgb == null) {
        NixUraPalette.streakStops
    } else {
        ShadeRamp.stops(colorArgb, config.stopCount).map { Color(it.toInt()) }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, c.border, RoundedCornerShape(12.dp))) {
            stops.forEach { Box(Modifier.weight(1f).height(34.dp).background(it)) }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(s.shadeDayZero, style = NixType.Small, color = c.textMuted, modifier = Modifier.weight(1f))
            Text(s.shadeDayThirty, style = NixType.Small, color = c.textMuted)
        }
    }
}

/** Compact "12d 04:11:33" text used where space is tight. */
fun compactElapsed(duration: Duration): String = formatElapsed(duration)
