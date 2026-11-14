package com.adhamelgendy.nixura.presentation.screen

import com.adhamelgendy.nixura.domain.usecase.DeletableItem
import com.adhamelgendy.nixura.presentation.deletion.rememberDeleteFlow
import com.adhamelgendy.nixura.domain.money.CountableHabits
import com.adhamelgendy.nixura.presentation.util.rememberNowMillis
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import com.adhamelgendy.nixura.domain.coach.BodyMilestone
import androidx.compose.runtime.key
import com.adhamelgendy.nixura.presentation.platform.rememberReducedMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.calculator.toCounterParts
import com.adhamelgendy.nixura.domain.coach.CoachEngine
import com.adhamelgendy.nixura.domain.coach.durationText
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.presentation.appearance.StreakColorProgressionConfig
import com.adhamelgendy.nixura.presentation.appearance.cardInk
import com.adhamelgendy.nixura.presentation.appearance.prefersLightContentOnCard
import com.adhamelgendy.nixura.presentation.components.CounterBlocks
import com.adhamelgendy.nixura.presentation.components.ConfirmDialog
import com.adhamelgendy.nixura.presentation.components.ErrorBanner
import com.adhamelgendy.nixura.presentation.components.GlyphBadge
import com.adhamelgendy.nixura.presentation.components.AddToHomeScreenButton
import com.adhamelgendy.nixura.domain.widget.WidgetKind
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconButton
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.components.NixProgressBar
import com.adhamelgendy.nixura.presentation.components.SectionLabel
import com.adhamelgendy.nixura.presentation.components.SubScreen
import com.adhamelgendy.nixura.presentation.components.icon
import com.adhamelgendy.nixura.presentation.components.InfoSheet
import com.adhamelgendy.nixura.presentation.components.habitCardFill
import com.adhamelgendy.nixura.presentation.components.habitCardGradient
import com.adhamelgendy.nixura.presentation.components.pressable
import com.adhamelgendy.nixura.presentation.habits.HabitsViewModel
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.theme.NixUraPalette
import com.adhamelgendy.nixura.presentation.util.formatDate
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration

@Composable
fun BadHabitDetailScreen(habitId: Long, navigator: Navigator) {
    val vm: HabitsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val engine = koinInject<CoachEngine>()
    val days = koinInject<DayCalculator>()
    val countable = koinInject<CountableHabits>()
    val now by rememberNowMillis()
    val s = LocalStrings.current
    val c = Nix.colors
    val model = state.habits.firstOrNull { it.habitId == habitId }
    var confirmRelapse by remember { mutableStateOf(false) }
    val deleteFlow = rememberDeleteFlow(onDeleted = { navigator.back() })
    var clearInfo by remember { mutableStateOf(false) }

    SubScreen(
        title = model?.name ?: "",
        onBack = { navigator.back() },
        actions = {
            if (model != null) {
                NixIconButton(NixIcon.Pencil, { navigator.push(Route.BadHabitForm(habitId)) }, size = 40.dp)
                Spacer(Modifier.width(8.dp))
                NixIconButton(NixIcon.Trash, { deleteFlow.confirm(DeletableItem.BadHabit(model.habitId), model.name) }, size = 40.dp, tint = c.danger)
            }
        },
    ) {
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }
        if (model == null) {
            if (!state.isLoading) Text(s.habitNotFound, style = NixType.Body, color = c.textSecondary)
            return@SubScreen
        }

        val gradient = habitCardGradient(model)
        val lightOnCard = gradient.middle.prefersLightContentOnCard()
        val ink = gradient.middle.cardInk()
        val primary = ink.primary
        val muted = ink.muted
        val category = remember(model.name) { engine.categoryFor(model.name) }
        val insight = engine.insightFor(model.name, model.activeElapsed)

        // Hero
        Column(
            Modifier
                .fillMaxWidth()
                .pressable(pressedScale = 0.98f, haptic = true) { clearInfo = true }
                .habitCardFill(gradient, RoundedCornerShape(28.dp))
                .border(1.dp, if (lightOnCard) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphBadge(model.topic.icon(), primary, primary.copy(alpha = 0.14f), size = 46.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(model.name, style = NixType.Title, color = primary)
                    Text(category.label, style = NixType.Small, color = muted)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.clearFor.uppercase(), style = NixType.Eyebrow, color = muted)
                CounterBlocks(model.activeElapsed.toCounterParts(), primary, muted, valueSize = 44, gap = 18.dp)
            }
            // Progress to the next card shade
            val bounds = StreakColorProgressionConfig.Default.segmentUpperBounds
            val nextBound = bounds.firstOrNull { it > model.activeElapsed }
            val prevBound = bounds.lastOrNull { it <= model.activeElapsed } ?: Duration.ZERO
            if (nextBound != null) {
                val progress = ((model.activeElapsed - prevBound) / (nextBound - prevBound)).toFloat().coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NixProgressBar(progress, color = primary, track = primary.copy(alpha = 0.18f), height = 6.dp)
                    Text(s.nextShadeIn(durationText(nextBound - model.activeElapsed)), style = NixType.Small, color = muted)
                }
            } else {
                Text(s.shadeMax, style = NixType.Small, color = muted)
            }
        }

        AddToHomeScreenButton(WidgetKind.QUIT_COUNTER, model.habitId)

        // Stats
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(s.statCurrent, durationText(model.activeElapsed), Modifier.weight(1f), info = s.infoClearFor)
            StatTile(s.statBest, durationText(model.longestStreak), Modifier.weight(1f), info = s.infoBestQuit)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(s.statRelapses, model.relapseCount.toString(), Modifier.weight(1f), info = s.infoRelapses)
            StatTile(
                s.statTrackingSince,
                model.trackingStartEpochMillis?.let { formatDate(days.dayIndexOf(it), s) } ?: "—",
                Modifier.weight(1f),
                info = s.infoTrackingSince,
            )
        }

        // Money saved (countable habits only)
        if (model.cost != null) {
            MoneySavedSection(model, now) { navigator.push(Route.BadHabitForm(habitId)) }
        } else if (remember(model.name) { countable.profileFor(model.name) } != null) {
            MoneySavedInvite { navigator.push(Route.BadHabitForm(habitId)) }
        }

        // Body & mind timeline
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(s.timelineTitle)
            if (category.milestones.isNotEmpty()) {
                RecoveryTimeline(category.milestones, model.activeElapsed, insight.next, insight.progressToNext)
            }
            Text(s.medicalDisclaimer, style = NixType.Smaller, color = c.textMuted)
        }

        // Relapse history
        if (model.relapseTimestamps.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(s.relapseHistory)
                NixCard {
                    model.relapseTimestamps.take(10).forEachIndexed { index, ts ->
                        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            NixIconView(NixIcon.Reset, c.textMuted, size = 16.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(formatDate(days.dayIndexOf(ts), s, includeWeekday = true), style = NixType.Body, color = c.textSecondary)
                        }
                    }
                }
            }
        }

        NixButton(s.recordRelapse, { confirmRelapse = true }, style = NixButtonStyle.Danger, icon = NixIcon.Reset, modifier = Modifier.fillMaxWidth())
        Text(s.relapseHint, style = NixType.Smaller, color = c.textMuted)
    }

    if (clearInfo && model != null) {
        InfoSheet(s.clearFor, durationText(model.activeElapsed), s.infoClearFor, onDismiss = { clearInfo = false })
    }
    if (confirmRelapse && model != null) {
        ConfirmDialog(
            title = s.relapseConfirmTitle,
            message = s.relapseConfirmBody,
            confirmText = s.recordRelapse,
            cancelText = s.cancel,
            danger = true,
            onConfirm = {
                vm.onRecordRelapse(model.habitId)
                confirmRelapse = false
            },
            onDismiss = { confirmRelapse = false },
        )
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, info: String? = null) {
    val c = Nix.colors
    var open by remember { mutableStateOf(false) }
    NixCard(modifier, padding = PaddingValues(16.dp), onClick = if (info != null) ({ open = true }) else null) {
        Text(label.uppercase(), style = NixType.Eyebrow, color = c.textMuted, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(value, style = NixType.Heading, color = c.textPrimary, maxLines = 1)
    }
    if (open && info != null) InfoSheet(label, value, info, onDismiss = { open = false })
}

/**
 * The recovery milestones as a vertical timeline: reached ones are ticked on a green rail, the next one pulses and
 * fills its stretch of the rail as time passes, later ones wait in grey.
 */
@Composable
private fun RecoveryTimeline(milestones: List<BodyMilestone>, elapsed: Duration, next: BodyMilestone?, progressToNext: Float) {
    val s = LocalStrings.current
    val c = Nix.colors
    val reached = milestones.count { it.after <= elapsed }
    val overall by animateFloatAsState(if (milestones.isEmpty()) 0f else reached.toFloat() / milestones.size, tween(600), label = "overall")
    NixCard {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(s.timelineMilestones.uppercase(), style = NixType.Eyebrow, color = c.textMuted, modifier = Modifier.weight(1f))
            Text(s.timelineReachedCount(reached, milestones.size), style = NixType.Heading, color = c.textPrimary)
        }
        Spacer(Modifier.height(10.dp))
        NixProgressBar(overall, color = c.positive, height = 6.dp)
        Spacer(Modifier.height(18.dp))
        // Long runs of reached steps fold away so the step being worked on stays in view.
        var showEarlier by remember { mutableStateOf(false) }
        val hidden = if (showEarlier) 0 else (reached - VISIBLE_REACHED).coerceAtLeast(0)
        if (reached > VISIBLE_REACHED) {
            EarlierStepsToggle(count = reached - VISIBLE_REACHED, expanded = showEarlier) { showEarlier = !showEarlier }
        }
        milestones.forEachIndexed { index, milestone ->
            if (index < hidden) return@forEachIndexed
            val state = when {
                milestone.after <= elapsed -> StepState.Reached
                milestone == next -> StepState.Next
                else -> StepState.Later
            }
            // The stretch of rail below a step leads to the following step: green once that one is reached, filling
            // up while it is the one being worked on.
            val following = milestones.getOrNull(index + 1)
            val railFill = when {
                following == null -> null
                following.after <= elapsed -> 1f
                following == next && state == StepState.Reached -> progressToNext
                else -> 0f
            }
            key(milestone) { TimelineStep(milestone, state, elapsed, railFill, progressToNext) }
        }
        if (next == null && milestones.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(s.timelineAllReached, style = NixType.SmallStrong, color = c.positive)
        }
    }
}

private enum class StepState { Reached, Next, Later }

/** Reached steps shown before the fold; the rest open with [EarlierStepsToggle]. */
private const val VISIBLE_REACHED = 2

/** A "+7" node on the rail that shows or hides the earlier reached steps. */
@Composable
private fun EarlierStepsToggle(count: Int, expanded: Boolean, onClick: () -> Unit) {
    val s = LocalStrings.current
    val c = Nix.colors
    val node = 28.dp
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .pressable(pressedScale = 0.98f, onClick = onClick),
    ) {
        Box(
            Modifier
                .width(node)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    drawLine(c.positive, Offset(x, node.toPx()), Offset(x, size.height), 2.dp.toPx(), StrokeCap.Round)
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                Modifier.size(node).clip(CircleShape).background(c.positiveSoft).border(1.dp, c.positive, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (expanded) NixIconView(NixIcon.Minus, c.positive, size = 14.dp, strokeWidth = 2.4f)
                else Text(s.timelineEarlierCount(count), style = NixType.Smaller, color = c.positive)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            if (expanded) s.timelineHideEarlier else s.timelineShowEarlier(count),
            style = NixType.SmallStrong,
            color = c.positive,
            modifier = Modifier.weight(1f).padding(top = 5.dp, bottom = 18.dp),
        )
    }
}

@Composable
private fun TimelineStep(milestone: BodyMilestone, state: StepState, elapsed: Duration, railFill: Float?, progressToNext: Float) {
    val s = LocalStrings.current
    val c = Nix.colors
    val node = 28.dp
    val fill by animateFloatAsState(railFill ?: 0f, tween(600), label = "rail")
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Rail + node
        Box(
            Modifier
                .width(node)
                .fillMaxHeight()
                .drawBehind {
                    if (railFill == null) return@drawBehind
                    val x = size.width / 2
                    val top = node.toPx()
                    val bottom = size.height
                    val stroke = 2.dp.toPx()
                    drawLine(c.border, Offset(x, top), Offset(x, bottom), stroke, StrokeCap.Round)
                    if (fill > 0f) {
                        val end = top + (bottom - top) * fill
                        drawLine(
                            Brush.verticalGradient(listOf(c.positive, if (fill < 1f) c.accent else c.positive), startY = top, endY = end),
                            Offset(x, top), Offset(x, end), stroke, StrokeCap.Round,
                        )
                    }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            TimelineNode(state, node)
        }
        Spacer(Modifier.width(14.dp))
        val content = Modifier.weight(1f).padding(bottom = if (railFill == null) 0.dp else 18.dp)
        if (state == StepState.Next) {
            Column(
                content
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.accent.copy(alpha = 0.10f))
                    .border(1.dp, c.accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.timelineUpNext.uppercase(), style = NixType.Eyebrow, color = c.accent, modifier = Modifier.weight(1f))
                    Text(s.timelineIn(durationText(milestone.after - elapsed)), style = NixType.SmallStrong, color = c.accent)
                }
                Spacer(Modifier.height(6.dp))
                Text(milestone.title, style = NixType.Heading, color = c.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(milestone.detail, style = NixType.Small, color = c.textSecondary)
                Spacer(Modifier.height(12.dp))
                NixProgressBar(progressToNext, height = 6.dp)
            }
        } else {
            val reached = state == StepState.Reached
            Column(content.padding(top = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.timelineAfter(durationText(milestone.after)).uppercase(), style = NixType.Eyebrow, color = c.textMuted, modifier = Modifier.weight(1f))
                    Text(
                        if (reached) s.reached else s.timelineIn(durationText(milestone.after - elapsed)),
                        style = NixType.SmallStrong,
                        color = if (reached) c.positive else c.textMuted,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(milestone.title, style = NixType.BodyStrong, color = if (reached) c.textPrimary else c.textSecondary)
                Spacer(Modifier.height(2.dp))
                Text(milestone.detail, style = NixType.Small, color = c.textMuted)
            }
        }
    }
}

@Composable
private fun TimelineNode(state: StepState, size: Dp) {
    val c = Nix.colors
    when (state) {
        StepState.Reached -> Box(
            Modifier.size(size).clip(CircleShape).background(c.positive),
            contentAlignment = Alignment.Center,
        ) { NixIconView(NixIcon.Tick, c.onAccent, size = 16.dp, strokeWidth = 2.4f) }
        StepState.Next -> {
            // A soft halo breathes around the step being worked on (a steady one when motion is reduced).
            val still = rememberReducedMotion()
            val pulse = rememberInfiniteTransition(label = "next")
            val wave by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "halo")
            val t = if (still) 0.5f else wave
            Box(
                Modifier
                    .size(size)
                    .drawBehind {
                        drawCircle(c.accent.copy(alpha = 0.35f * (1f - t)), radius = this.size.minDimension / 2 * (1f + 0.6f * t))
                    }
                    .clip(CircleShape)
                    .background(c.surface)
                    .border(2.dp, c.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(10.dp).clip(CircleShape).background(c.accent)) }
        }
        StepState.Later -> Box(
            Modifier.size(size).clip(CircleShape).background(c.surfaceRaised).border(1.dp, c.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(6.dp).clip(CircleShape).background(c.textMuted.copy(alpha = 0.6f))) }
    }
}
