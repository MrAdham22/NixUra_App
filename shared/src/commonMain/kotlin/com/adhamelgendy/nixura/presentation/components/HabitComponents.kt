package com.adhamelgendy.nixura.presentation.components

import com.adhamelgendy.nixura.domain.coach.durationText
import com.adhamelgendy.nixura.presentation.util.forDisplay
import com.adhamelgendy.nixura.presentation.util.weekdayLettersSundayFirst
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.adhamelgendy.nixura.presentation.appearance.relativeLuminance
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.adhamelgendy.nixura.presentation.util.timeLeft
import com.adhamelgendy.nixura.domain.calculator.MilestoneCalculator
import com.adhamelgendy.nixura.domain.recognition.PrayerHabitDetector
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.BoxWithConstraints
import com.adhamelgendy.nixura.presentation.appearance.CardGradient
import com.adhamelgendy.nixura.presentation.appearance.goodFillColors
import com.adhamelgendy.nixura.presentation.platform.rememberReducedMotion
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
import com.adhamelgendy.nixura.domain.calculator.CounterParts
import com.adhamelgendy.nixura.domain.calculator.CounterUnit
import com.adhamelgendy.nixura.domain.calculator.DayStatus
import com.adhamelgendy.nixura.domain.calculator.GoodHabitStatsCalculator
import com.adhamelgendy.nixura.domain.calculator.toCounterParts
import com.adhamelgendy.nixura.domain.coach.CoachEngine
import com.adhamelgendy.nixura.domain.recognition.GoodHabitFillStyle
import com.adhamelgendy.nixura.domain.money.Currencies
import com.adhamelgendy.nixura.domain.model.EventSource
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.presentation.appearance.ShadeRamp
import com.adhamelgendy.nixura.presentation.appearance.StreakColorMapper
import com.adhamelgendy.nixura.presentation.appearance.StreakColorProgressionConfig
import com.adhamelgendy.nixura.presentation.appearance.cardInk
import com.adhamelgendy.nixura.presentation.appearance.prefersLightContentOnCard
import com.adhamelgendy.nixura.presentation.goodhabits.GoodHabitUi
import com.adhamelgendy.nixura.presentation.habits.HabitCardUiModel
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.strings.Strings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.theme.NixUraPalette
import com.adhamelgendy.nixura.presentation.util.formatElapsed
import org.koin.compose.koinInject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

/**
 * Days → hours → minutes → seconds blocks. Shows from the largest non-zero unit down to seconds. The digits shrink
 * when they would not fit the width (big counters, large system font), so the seconds are never cut off.
 */
@Composable
fun CounterBlocks(
    parts: CounterParts,
    color: Color,
    mutedColor: Color,
    valueSize: Int,
    modifier: Modifier = Modifier,
    gap: Dp = 16.dp,
    /** Which units to show; quit counters show every unit, far-off events drop the seconds. */
    units: List<CounterUnit> = parts.visibleUnits,
) {
    val s = LocalStrings.current
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        // Measured with the widest digit, so the size stays steady while the seconds tick.
        val sample = units.joinToString("") { unit ->
            if (unit == CounterUnit.DAYS) "8".repeat(parts.valueOf(unit).toString().length) else "88"
        }
        val needed = measurer.measure(sample, NixType.counter(valueSize)).size.width +
            with(density) { (gap * (units.size - 1)).toPx() }
        val available = constraints.maxWidth.toFloat()
        val fitted = if (constraints.hasBoundedWidth && needed > available) (valueSize * available / needed).toInt() else valueSize
        // Read as one phrase ("3 days, 4 hours, ...") instead of "3, DAYS, 04, HRS". Not a live region, so a
        // screen reader reads it when focused and never announces every tick.
        val spoken = s.counterSpoken(
            parts.valueOf(CounterUnit.DAYS).takeIf { CounterUnit.DAYS in units },
            parts.valueOf(CounterUnit.HOURS).takeIf { CounterUnit.HOURS in units },
            parts.valueOf(CounterUnit.MINUTES).takeIf { CounterUnit.MINUTES in units },
            parts.valueOf(CounterUnit.SECONDS).takeIf { CounterUnit.SECONDS in units },
        )
        Box(Modifier.clearAndSetSemantics { contentDescription = spoken }) {
            CounterBlockRow(parts, units, color, mutedColor, fitted, gap, s)
        }
    }
}

@Composable
private fun CounterBlockRow(parts: CounterParts, units: List<CounterUnit>, color: Color, mutedColor: Color, valueSize: Int, gap: Dp, s: Strings) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Bottom) {
        units.forEach { unit ->
            Column {
                val value = parts.valueOf(unit)
                Text(
                    text = if (unit == CounterUnit.DAYS) value.toString() else value.toString().padStart(2, '0'),
                    style = NixType.counter(valueSize),
                    softWrap = false,
                    color = color,
                    maxLines = 1,
                )
                Text(unitLabel(unit, s).uppercase(), style = NixType.Eyebrow, color = mutedColor, maxLines = 1)
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

/** Live card colors for a bad habit: default brand progression or the user's chosen color ramp. */
@Composable
fun habitCardGradient(model: HabitCardUiModel): CardGradient {
    val mapper = rememberStreakMapper()
    val target = mapper.cardGradient(model.activeElapsed, model.colorArgb)
    val start by animateColorAsState(target.start, tween(900))
    val middle by animateColorAsState(target.middle, tween(900))
    val end by animateColorAsState(target.end, tween(900))
    return CardGradient(start, middle, end)
}

/** Paints a habit card: its diagonal gradient plus a soft light in the top corner, so it never reads as a flat block. */
fun Modifier.habitCardFill(gradient: CardGradient, shape: Shape): Modifier = this
    .clip(shape)
    .drawWithCache {
        val diagonal = Brush.linearGradient(listOf(gradient.start, gradient.middle, gradient.end), Offset.Zero, Offset(size.width, size.height))
        val glowColor = if (gradient.middle.prefersLightContentOnCard()) Color.White.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.28f)
        val glow = Brush.radialGradient(
            listOf(glowColor, Color.Transparent),
            center = Offset(size.width * 0.9f, 0f),
            radius = size.maxDimension * 0.75f,
        )
        onDrawBehind {
            drawRect(diagonal)
            drawRect(glow)
        }
    }

/**
 * How far today's target is reached, as colors. Plain habits fill with green and are solid green once done; water
 * habits fill with rising water ([paint]). [background] is the card's average tone, used to pick readable content.
 */
data class GoodFill(
    val fill: Float,
    val background: Color,
    val content: Color,
    val contentMuted: Color,
    val doneSquare: Color,
    val paint: Modifier,
)

@Composable
fun rememberGoodFill(todayCount: Int, dailyTarget: Int, style: GoodHabitFillStyle = GoodHabitFillStyle.PLAIN): GoodFill {
    val c = Nix.colors
    val target = GoodHabitStatsCalculator.fillLevel(todayCount, dailyTarget)
    val fill by animateFloatAsState(target, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow), label = "goodFill")
    if (style == GoodHabitFillStyle.PRAYER) {
        val art = rememberPrayerArt(fill, todayCount)
        return GoodFill(fill, art.background, art.content, art.contentMuted, art.doneSquare, art.modifier)
    }
    val water = style == GoodHabitFillStyle.WATER
    val colors = goodFillColors(fill, water, c)
    val paint = if (water) {
        Modifier.liquidFill(target, LiquidColors(empty = c.surface, surface = colors.liquidSurface, depth = colors.liquidDepth))
    } else {
        Modifier.background(colors.background)
    }
    return GoodFill(fill, colors.background, colors.content, colors.contentMuted, colors.doneSquare, paint)
}

@Composable
fun BadHabitCard(
    model: HabitCardUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    /** "Craving? Talk it through": opens the coach about this habit. Hidden when null. */
    onCraving: (() -> Unit)? = null,
) {
    val s = LocalStrings.current
    val engine = koinInject<CoachEngine>()
    val gradient = habitCardGradient(model)
    val ink = gradient.middle.cardInk()
    val primary = ink.primary
    val muted = ink.muted
    val shape = RoundedCornerShape(26.dp)
    val category = remember(model.name) { engine.categoryFor(model.name) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.98f, onLongClick = onLongClick, onClick = onClick)
            .habitCardLift(gradient, shape)
            .habitCardFill(gradient, shape)
            .border(1.dp, habitCardEdge(gradient), shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphBadge(model.topic.icon(), primary, primary.copy(alpha = 0.14f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(model.name.forDisplay(), style = NixType.Heading, color = primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(category.label, style = NixType.Small, color = muted, maxLines = 1)
            }
            if (onLongClick != null) {
                // The same options as a long press, for people who don't know to hold the card.
                NixIconButton(
                    NixIcon.More,
                    onLongClick,
                    modifier = Modifier.semantics { contentDescription = s.cardMoreOptions },
                    tint = primary,
                    background = primary.copy(alpha = 0.10f),
                    size = 40.dp,
                    bordered = false,
                )
            } else {
                NixIconView(NixIcon.Chevron, muted, size = 20.dp)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s.clearFor.uppercase(), style = NixType.Eyebrow, color = muted)
            CounterBlocks(model.activeElapsed.toCounterParts(), primary, muted, valueSize = 30)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val next = MilestoneCalculator.next(model.activeElapsed)
            ProgressStageIndicator(
                stageCount = model.stageCount,
                activeStageIndex = model.activeStageIndex,
                label = s.nextMilestone(s.milestoneName(next.days), s.timeLeft(next.remaining)),
                labelColor = muted,
                activeStageColor = primary,
                inactiveStageColor = muted,
                modifier = Modifier.weight(1f),
            )
        }
        model.moneySaved?.let { saved ->
            val cost = model.cost ?: return@let
            Text(
                s.moneyCardLine(Currencies.format(saved.savedMinor, saved.currency), s.unitsAvoided(saved.unitsAvoided, cost.unit)),
                style = NixType.SmallStrong,
                color = primary,
                maxLines = 2,
                modifier = Modifier.padding(top = 0.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (model.isLongestYet()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NixIconView(NixIcon.Flame, primary, size = 16.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(s.longestYet, style = NixType.SmallStrong, color = primary, maxLines = 2)
                    }
                } else {
                    Text(s.longestLabel(model.longestStreakLabel), style = NixType.Small, color = muted, maxLines = 2)
                }
            }
            if (onCraving != null) {
                Spacer(Modifier.width(10.dp))
                CravingChip(primary, onCraving)
            }
        }
    }
}

/**
 * A quit habit as a short row for Home: the same streak-colored card, with the name, the next milestone, the live
 * counter and "Craving?". The full card (money, longest streak) stays on the Quit tab.
 */
@Composable
fun QuitHabitRow(
    model: HabitCardUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onCraving: (() -> Unit)? = null,
) {
    val s = LocalStrings.current
    val gradient = habitCardGradient(model)
    val ink = gradient.middle.cardInk()
    val shape = RoundedCornerShape(22.dp)
    val next = MilestoneCalculator.next(model.activeElapsed)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.98f, onLongClick = onLongClick, onClick = onClick)
            .habitCardLift(gradient, shape)
            .habitCardFill(gradient, shape)
            .border(1.dp, habitCardEdge(gradient), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphBadge(model.topic.icon(), ink.primary, ink.primary.copy(alpha = 0.14f), size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(model.name.forDisplay(), style = NixType.BodyStrong, color = ink.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    s.nextMilestone(s.milestoneName(next.days), s.timeLeft(next.remaining)),
                    style = NixType.Small,
                    color = ink.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CounterBlocks(model.activeElapsed.toCounterParts(), ink.primary, ink.muted, valueSize = 24, gap = 14.dp, modifier = Modifier.weight(1f))
            if (onCraving != null) {
                Spacer(Modifier.width(10.dp))
                CravingChip(ink.primary, onCraving)
            }
        }
    }
}

/** Small "Craving?" button on a quit card; read out as the full "Craving? Talk it through". */
@Composable
private fun CravingChip(ink: Color, onClick: () -> Unit) {
    val s = LocalStrings.current
    val pill = RoundedCornerShape(50)
    Row(
        Modifier
            .pressable(pressedScale = 0.95f, haptic = true, onClick = onClick)
            .clearAndSetSemantics { contentDescription = s.cravingAction; role = Role.Button }
            .heightIn(min = 44.dp)
            .clip(pill)
            .background(ink.copy(alpha = 0.14f), pill)
            .border(1.dp, ink.copy(alpha = 0.22f), pill)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NixIconView(NixIcon.Sparkle, ink, size = 16.dp)
        Spacer(Modifier.width(6.dp))
        Text(s.cravingShort, style = NixType.SmallStrong, color = ink, maxLines = 1)
    }
}

/**
 * Lifts a habit card off the page: a soft shadow whose glow takes the color the card is growing into, so even a
 * day-0 navy card separates from the navy background (DESIGN.md, Elevation & Depth).
 */
fun Modifier.habitCardLift(gradient: CardGradient, shape: Shape): Modifier =
    softLift(shape, gradient.end)

/** The card's 1dp edge: stronger on the darkest stages, where the card is close to the page color. */
fun habitCardEdge(gradient: CardGradient): Color = when {
    !gradient.middle.prefersLightContentOnCard() -> Color.Black.copy(alpha = 0.10f)
    gradient.middle.relativeLuminance() < 0.05f -> Color.White.copy(alpha = 0.24f)
    else -> Color.White.copy(alpha = 0.14f)
}

/** The current streak is the best this habit has had (the longest streak includes the one running now). */
fun HabitCardUiModel.isLongestYet(): Boolean = activeElapsed > Duration.ZERO && activeElapsed >= longestStreak

// ---------- Good habits: day squares, week strip, month calendar ----------

/** One day. The more of the target was reached, the more colored the square is, up to solid when done. */
@Composable
fun DaySquare(
    count: Int,
    target: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
    isToday: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    doneColor: Color = Nix.colors.positive,
    idleColor: Color = Nix.colors.surfaceRaised,
    todayBorder: Color = Nix.colors.textSecondary,
    onClick: (() -> Unit)? = null,
) {
    val c = Nix.colors
    val shape = RoundedCornerShape(9.dp)
    val status = GoodHabitStatsCalculator.statusOf(count, target)
    val ratio = (count.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    val bg by animateColorAsState(if (enabled) lerp(idleColor, doneColor, ratio) else Color.Transparent, tween(350), label = "daySquare")
    val borderColor = when {
        selected -> c.accent
        isToday -> todayBorder
        !enabled -> c.border.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    val textColor = when {
        !enabled -> c.textMuted.copy(alpha = 0.5f)
        status == DayStatus.DONE || ratio >= 0.6f -> Color(0xFF0E2A1D)
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

/**
 * Last 7 days, oldest first, ending today. With [weekStartIndex] it shows that week instead (Sunday to Saturday, for a
 * weekly habit), with the days still to come dimmed.
 */
@Composable
fun WeekStrip(
    countsByDay: Map<Long, Int>,
    target: Int,
    todayIndex: Long,
    createdDayIndex: Long,
    modifier: Modifier = Modifier,
    labelColor: Color = Nix.colors.textMuted,
    doneColor: Color = Nix.colors.positive,
    idleColor: Color = Nix.colors.surfaceRaised,
    todayBorder: Color = Nix.colors.textSecondary,
    weekStartIndex: Long? = null,
) {
    val s = LocalStrings.current
    val firstDay = weekStartIndex ?: (todayIndex - 6)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until 7) {
            val day = firstDay + i
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(s.weekdayLetters[CivilCalendar.isoWeekday(day) - 1], style = NixType.Small, color = labelColor)
                DaySquare(
                    count = countsByDay[day] ?: 0,
                    target = target,
                    modifier = Modifier.fillMaxWidth(),
                    isToday = day == todayIndex,
                    enabled = if (weekStartIndex != null) day <= todayIndex else day >= createdDayIndex - 6,
                    doneColor = doneColor,
                    idleColor = idleColor,
                    todayBorder = todayBorder,
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
    val lead = CivilCalendar.sundayFirstColumn(firstDay)
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
            s.weekdayLettersSundayFirst().forEach {
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
                            count = countsByDay[day] ?: 0,
                            target = target,
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
    onLongClick: (() -> Unit)? = null,
) {
    val s = LocalStrings.current
    val c = Nix.colors
    val look = rememberGoodFill(habit.progressCount, habit.progressTarget, habit.fillStyle)
    val pulse = rememberCompletionPulse(habit.goalReached)
    var streakInfo by remember { mutableStateOf(false) }
    NixCard(
        modifier.fillMaxWidth().graphicsLayer { scaleX = pulse.value; scaleY = pulse.value },
        onClick = onClick,
        onLongClick = onLongClick,
        paint = look.paint,
        lift = look.background,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (habit.fillStyle == GoodHabitFillStyle.PRAYER) {
                PrayerArchBadge(look)
            } else {
                GlyphBadge(habit.topic.icon(), if (look.fill > 0f) look.content else c.accent, (if (look.fill > 0f) look.content else c.accent).copy(alpha = 0.14f))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name.forDisplay(), style = NixType.Heading, color = look.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(goodProgressText(habit, s), style = NixType.Small, color = look.contentMuted)
            }
            val prayer = habit.fillStyle == GoodHabitFillStyle.PRAYER
            GoodHabitButton(prayer, look, NixIcon.Minus, onMinus, size = 38.dp, tint = look.content, background = look.content.copy(alpha = 0.10f), secondary = true)
            Spacer(Modifier.width(8.dp))
            GoodHabitButton(
                prayer, look,
                if (habit.doneToday && (habit.dailyTarget == 1 || habit.isWeekly)) NixIcon.Tick else NixIcon.Plus,
                onPlus,
                size = 44.dp,
                tint = if (habit.doneToday) look.background else c.onAccent,
                background = if (habit.doneToday) look.content else c.accent,
                haptic = true,
            )
        }
        if (habit.fillStyle == GoodHabitFillStyle.PRAYER && !habit.isWeekly && habit.dailyTarget in 2..MAX_PRAYER_BEADS) {
            Spacer(Modifier.height(12.dp))
            PrayerBeads(habit.todayCount, habit.dailyTarget, look, if (habit.dailyTarget == PrayerHabitDetector.DAILY_PRAYERS) s.prayerNames else emptyList())
        }
        Spacer(Modifier.height(14.dp))
        WeekStrip(
            habit.countsByDay, if (habit.isWeekly) 1 else habit.dailyTarget, todayIndex, habit.createdDayIndex,
            labelColor = look.contentMuted,
            doneColor = look.doneSquare,
            idleColor = look.content.copy(alpha = 0.10f),
            todayBorder = look.content.copy(alpha = 0.7f),
            weekStartIndex = if (habit.isWeekly) habit.weekStartIndex else null,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().pressable(pressedScale = 0.96f, haptic = true) { streakInfo = true }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (habit.currentStreak > 0) {
                NixIconView(NixIcon.Flame, if (look.fill >= 0.6f) look.content else c.accentWarm, size = 16.dp)
                Spacer(Modifier.width(6.dp))
                Text(if (habit.isWeekly) s.streakWeeks(habit.currentStreak) else s.streakDays(habit.currentStreak), style = NixType.SmallStrong, color = look.content)
            } else {
                // No streak yet (or it broke): an invitation, not a bold "0 day streak" with a flame.
                Text(if (habit.isWeekly) s.streakStartWeek else s.streakStartToday, style = NixType.Small, color = look.contentMuted)
            }
            Spacer(Modifier.weight(1f))
            Text(if (habit.isWeekly) s.daysDoneTotal(habit.totalTimesDone) else s.timesDoneTotal(habit.totalTimesDone), style = NixType.Small, color = look.contentMuted)
        }
        // Room at the bottom of a prayer card for its mosque, so it stands clear of the text
        if (habit.fillStyle == GoodHabitFillStyle.PRAYER) Spacer(Modifier.height(48.dp))
    }
    if (streakInfo) {
        if (habit.isWeekly) {
            InfoSheet(s.statCurrentStreak, s.weeksCount(habit.currentStreak), s.infoCurrentStreakWeekly, onDismiss = { streakInfo = false })
        } else {
            InfoSheet(s.statCurrentStreak, s.daysCount(habit.currentStreak), s.infoCurrentStreak, onDismiss = { streakInfo = false })
        }
    }
}

/** Most stars a prayer card shows one by one; above this it relies on the counter alone. */
const val MAX_PRAYER_BEADS = 8

/** "3 / 8 today" for a daily habit, "2 / 3 days this week" for a weekly one. */
fun goodProgressText(habit: GoodHabitUi, s: Strings): String =
    if (habit.isWeekly) s.weekProgress(habit.progressCount, habit.progressTarget) else s.todayProgress(habit.todayCount, habit.dailyTarget)

/** A small celebratory bounce, with a firm tap, the moment a habit's target is reached. */
@Composable
fun rememberCompletionPulse(done: Boolean): Animatable<Float, AnimationVector1D> {
    val scale = remember { Animatable(1f) }
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotion()
    var wasDone by remember { mutableStateOf(done) }
    LaunchedEffect(done) {
        if (done && !wasDone) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!reducedMotion) {
                scale.animateTo(1.03f, tween(110))
                scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
            }
        }
        wasDone = done
    }
    return scale
}

// ---------- Events ----------

@Composable
fun EventCard(
    event: Event,
    nowMillis: Long,
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val s = LocalStrings.current
    val c = Nix.colors
    val remaining = (event.targetAtEpochMillis - nowMillis).milliseconds
    val passed = remaining < Duration.ZERO
    val parts = (if (passed) -remaining else remaining).toCounterParts()
    NixCard(modifier.fillMaxWidth(), onClick = onClick, onLongClick = onLongClick, lift = Color.Black) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.categoryLabel(event.category).uppercase(), style = NixType.Eyebrow, color = if (passed) c.textMuted else c.textSecondary)
                Spacer(Modifier.height(4.dp))
                Text(event.title.forDisplay(), style = NixType.Heading, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(dateText, style = NixType.Small, color = c.textMuted)
            }
            // Where an imported event comes from (made-in-app events have no badge)
            val sourceName = when (event.source) {
                EventSource.GOOGLE -> s.sourceGoogle
                EventSource.NOTION -> s.sourceNotion
                EventSource.LOCAL -> null
            }
            if (sourceName != null) {
                Text(
                    sourceName,
                    style = NixType.SmallStrong,
                    color = c.textSecondary,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(c.surfaceRaised).padding(horizontal = 10.dp, vertical = 6.dp),
                )
                if (event.reminderLeadMillis.isNotEmpty()) Spacer(Modifier.width(6.dp))
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
            if (onLongClick != null) {
                // The same options as a long press, like the ⋯ on quit cards.
                Spacer(Modifier.width(6.dp))
                NixIconButton(
                    NixIcon.More,
                    onLongClick,
                    modifier = Modifier.semantics { contentDescription = s.cardMoreOptions },
                    tint = c.textSecondary,
                    background = c.surfaceRaised,
                    size = 40.dp,
                    bordered = false,
                )
            }
        }
        if (passed) {
            // A past event is history: one quiet line, no ticking counter.
            Spacer(Modifier.height(8.dp))
            Text(s.passedAgo(durationText(-remaining)), style = NixType.Small, color = c.textMuted)
        } else {
            Spacer(Modifier.height(14.dp))
            Text(s.startsIn.uppercase(), style = NixType.Eyebrow, color = c.textMuted)
            Spacer(Modifier.height(6.dp))
            CounterBlocks(parts, c.textPrimary, c.textMuted, valueSize = 26, units = parts.unitsWithoutFarSeconds)
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
