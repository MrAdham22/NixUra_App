package com.adhamelgendy.nixura.presentation.screen

import com.adhamelgendy.nixura.presentation.util.forDisplay
import com.adhamelgendy.nixura.domain.recognition.GoodHabitFillStyle
import com.adhamelgendy.nixura.domain.recognition.PrayerHabitDetector
import com.adhamelgendy.nixura.presentation.components.ringSweep
import com.adhamelgendy.nixura.presentation.components.GoodHabitButton
import com.adhamelgendy.nixura.presentation.components.PrayerBeads
import com.adhamelgendy.nixura.presentation.components.MAX_PRAYER_BEADS
import com.adhamelgendy.nixura.domain.usecase.DeletableItem
import com.adhamelgendy.nixura.presentation.deletion.rememberDeleteFlow
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.auth.SessionManager
import com.adhamelgendy.nixura.domain.auth.SessionState
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.presentation.coach.CoachViewModel
import com.adhamelgendy.nixura.presentation.components.BadHabitCard
import com.adhamelgendy.nixura.presentation.components.EventCard
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconButton
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.components.ScoreKind
import com.adhamelgendy.nixura.presentation.components.ScoreSheet
import com.adhamelgendy.nixura.presentation.components.SectionLabel
import com.adhamelgendy.nixura.presentation.components.TabScreen
import com.adhamelgendy.nixura.presentation.components.pressable
import com.adhamelgendy.nixura.presentation.components.rememberCompletionPulse
import com.adhamelgendy.nixura.presentation.components.goodProgressText
import com.adhamelgendy.nixura.presentation.components.rememberGoodFill
import com.adhamelgendy.nixura.presentation.events.EventsViewModel
import com.adhamelgendy.nixura.presentation.goodhabits.GoodHabitUi
import com.adhamelgendy.nixura.presentation.goodhabits.GoodHabitsViewModel
import com.adhamelgendy.nixura.presentation.guide.GuideTarget
import com.adhamelgendy.nixura.presentation.guide.guideTarget
import com.adhamelgendy.nixura.presentation.habits.HabitsViewModel
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.profile.ProfileStats
import com.adhamelgendy.nixura.presentation.profile.ProfileViewModel
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.util.formatDateTime
import com.adhamelgendy.nixura.presentation.util.rememberNowMillis
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val MAX_TODAY_ROWS = 4

/** The dashboard: progress at a glance, today's check-ins in one tap, then what's running and what's next. */
@Composable
fun HomeScreen(navigator: Navigator) {
    val habitsVm: HabitsViewModel = koinViewModel()
    val goodVm: GoodHabitsViewModel = koinViewModel()
    val eventsVm: EventsViewModel = koinViewModel()
    val profileVm: ProfileViewModel = koinViewModel()
    val coachVm: CoachViewModel = koinViewModel()
    val habits by habitsVm.uiState.collectAsStateWithLifecycle()
    val good by goodVm.uiState.collectAsStateWithLifecycle()
    val events by eventsVm.uiState.collectAsStateWithLifecycle()
    val profile by profileVm.uiState.collectAsStateWithLifecycle()
    val coach by coachVm.uiState.collectAsStateWithLifecycle()
    val session by koinInject<SessionManager>().state.collectAsStateWithLifecycle()
    val now by rememberNowMillis()
    val days = koinInject<DayCalculator>()
    val s = LocalStrings.current
    val c = Nix.colors
    var scoreSheet by remember { mutableStateOf<ScoreKind?>(null) }
    val voiceVm: com.adhamelgendy.nixura.presentation.voice.VoiceNoteViewModel = koinViewModel()
    val deleteFlow = rememberDeleteFlow()
    val quitActions = rememberQuitCardActions(navigator, deleteFlow)

    val hour = (days.millisOfDay(now) / 3_600_000L).toInt()
    val greeting = when {
        hour < 12 -> s.greetingMorning
        hour < 18 -> s.greetingAfternoon
        else -> s.greetingEvening
    }
    val nextEvent = events.events.firstOrNull { it.targetAtEpochMillis > now }
    val stats = profile.stats
    val nothingYet = habits.habits.isEmpty() && good.habits.isEmpty() && nextEvent == null

    TabScreen {
        Text(
            // First name only: a full name wraps the title onto two lines
            if (profile.name.isBlank()) greeting else "$greeting, ${profile.name.trim().substringBefore(' ').forDisplay()}",
            style = NixType.Title,
            color = c.textPrimary,
        )

        if (session is SessionState.SignedOut) GuestBanner { navigator.push(Route.Auth.Login) }

        if (voiceVm.supported) com.adhamelgendy.nixura.presentation.voice.VoiceCard(onClick = voiceVm::open)

        if (nothingYet) {
            StartCard(navigator)
        } else {
            ProgressCard(stats, onOpen = { scoreSheet = it })
        }

        if (good.habits.isNotEmpty()) {
            SectionLabel(s.homeToday) {
                Text(s.seeAll, style = NixType.SmallStrong, color = c.accent, modifier = Modifier.pressable { navigator.selectTab(Route.GoodHabits) })
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                good.habits.take(MAX_TODAY_ROWS).forEach { habit ->
                    TodayRow(
                        habit,
                        onOpen = { navigator.push(Route.GoodHabitDetail(habit.id)) },
                        onLongClick = { deleteFlow.open(DeletableItem.GoodHabit(habit.id), habit.name) },
                        onPlus = { goodVm.onChangeToday(habit.id, +1) },
                    )
                }
            }
        }

        if (habits.habits.isNotEmpty()) {
            SectionLabel(s.quittingSection) {
                Text(s.seeAll, style = NixType.SmallStrong, color = c.accent, modifier = Modifier.pressable { navigator.selectTab(Route.BadHabits) })
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                habits.habits.take(2).forEach {
                    BadHabitCard(
                        it,
                        onClick = { navigator.push(Route.BadHabitDetail(it.habitId)) },
                        onLongClick = { quitActions.openOptions(it) },
                        onCraving = { quitActions.craving(it) },
                    )
                }
            }
        }

        if (nextEvent != null) {
            SectionLabel(s.nextEventSection) {
                Text(s.seeAll, style = NixType.SmallStrong, color = c.accent, modifier = Modifier.pressable { navigator.selectTab(Route.Events) })
            }
            EventCard(
                nextEvent, now, formatDateTime(nextEvent.targetAtEpochMillis, days, s),
                onClick = { navigator.push(Route.EventForm(nextEvent.id)) },
                onLongClick = { deleteFlow.open(DeletableItem.Event(nextEvent.id), nextEvent.title) },
            )
        }

        coach.insight?.let { insight ->
            NixCard(Modifier.fillMaxWidth(), onClick = { navigator.selectTab(Route.Coach) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NixIconView(NixIcon.Sparkle, c.accentWarm, size = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.homeThought.uppercase(), style = NixType.Eyebrow, color = c.textMuted)
                        Spacer(Modifier.height(2.dp))
                        Text(insight.body, style = NixType.Small, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    NixIconView(NixIcon.Chevron, c.textMuted, size = 16.dp)
                }
            }
        }
    }

    scoreSheet?.let { kind -> ScoreSheet(kind, stats, onDismiss = { scoreSheet = null }) }
}

/** A clear way back to signing in for guests, and a reminder that their data is only on this phone. */
@Composable
private fun GuestBanner(onSignIn: () -> Unit) {
    val s = LocalStrings.current
    val c = Nix.colors
    NixCard(
        Modifier.fillMaxWidth(),
        onClick = onSignIn,
        color = c.accent.copy(alpha = 0.10f),
        borderColor = c.accent.copy(alpha = 0.30f),
        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.guestBannerTitle, style = NixType.SmallStrong, color = c.textPrimary)
                Text(s.guestBannerBody, style = NixType.Small, color = c.textSecondary)
            }
            Spacer(Modifier.width(10.dp))
            Text(s.authSignIn, style = NixType.SmallStrong, color = c.accent)
        }
    }
}

/** Shown instead of empty sections when there is nothing yet: one calm place to begin. */
@Composable
private fun StartCard(navigator: Navigator) {
    val s = LocalStrings.current
    val c = Nix.colors
    NixCard(Modifier.fillMaxWidth().guideTarget(GuideTarget.Progress)) {
        Text(s.homeStartTitle, style = NixType.Heading, color = c.textPrimary)
        Spacer(Modifier.height(10.dp))
        StartRow(NixIcon.Shield, s.homeStartQuit, c.accent) { navigator.push(Route.BadHabitForm()) }
        StartRow(NixIcon.CircleCheck, s.homeStartBuild, c.positive) { navigator.push(Route.GoodHabitForm()) }
        StartRow(NixIcon.Calendar, s.homeStartEvent, c.accentWarm) { navigator.push(Route.EventForm()) }
    }
}

@Composable
private fun StartRow(icon: NixIcon, label: String, tint: Color, onClick: () -> Unit) {
    val c = Nix.colors
    Row(
        Modifier.fillMaxWidth().pressable(pressedScale = 0.98f, onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NixIconView(icon, tint, size = 22.dp)
        Spacer(Modifier.width(14.dp))
        Text(label, style = NixType.Body, color = c.textPrimary, modifier = Modifier.weight(1f))
        NixIconView(NixIcon.Plus, c.textMuted, size = 18.dp)
    }
}

/** Two rings with Momentum in the middle. Every part opens the explanation of its number. */
@Composable
private fun ProgressCard(stats: ProfileStats, onOpen: (ScoreKind) -> Unit) {
    val s = LocalStrings.current
    val c = Nix.colors
    NixCard(Modifier.fillMaxWidth().guideTarget(GuideTarget.Progress)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(128.dp).pressable(pressedScale = 0.95f, haptic = true) { onOpen(ScoreKind.Momentum) },
                contentAlignment = Alignment.Center,
            ) {
                ScoreRings(stats.quitScore, stats.disciplineScore, Modifier.size(128.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stats.momentum?.let { "$it%" } ?: "—", style = NixType.counter(24), color = c.textPrimary)
                    Text(s.momentum, style = NixType.Smaller, color = c.textMuted)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ScoreLegend(s.quitScoreTitle, stats.quitScore, c.accentWarm) { onOpen(ScoreKind.BreakingFree) }
                ScoreLegend(s.disciplineScoreTitle, stats.disciplineScore, c.positive) { onOpen(ScoreKind.Discipline) }
                stats.disciplineDelta?.let {
                    Text(s.momentumDelta(it), style = NixType.Small, color = c.textMuted, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun ScoreLegend(label: String, score: Int?, color: Color, onClick: () -> Unit) {
    val c = Nix.colors
    Row(
        Modifier.fillMaxWidth().pressable(pressedScale = 0.97f, haptic = true, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = NixType.Small, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(score?.let { "$it%" } ?: "—", style = NixType.counter(22), color = if (score == null) c.textMuted else color)
        }
        NixIconView(NixIcon.Chevron, c.textMuted, size = 14.dp)
    }
}

@Composable
private fun ScoreRings(quit: Int?, discipline: Int?, modifier: Modifier = Modifier, stroke: Dp = 11.dp) {
    val c = Nix.colors
    val settle = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessVeryLow)
    val outer by animateFloatAsState((quit ?: 0) / 100f, settle, label = "quitRing")
    val inner by animateFloatAsState((discipline ?: 0) / 100f, settle, label = "disciplineRing")
    val accent = c.accentWarm
    val positive = c.positive
    Canvas(modifier) {
        val width = stroke.toPx()
        val gap = width + 3.dp.toPx()
        ring(accent, outer, width, inset = width / 2)
        ring(positive, inner, width, inset = width / 2 + gap)
    }
}

private fun DrawScope.ring(color: Color, progress: Float, width: Float, inset: Float) {
    val diameter = size.minDimension - inset * 2
    val topLeft = Offset(inset, inset)
    val arc = Size(diameter, diameter)
    // The track is the "empty" state, strong enough to read as a ring on its own.
    drawArc(color.copy(alpha = 0.26f), 0f, 360f, false, topLeft, arc, style = Stroke(width))
    val sweep = ringSweep(progress)
    if (sweep > 0f) drawArc(color, -90f, sweep, false, topLeft, arc, style = Stroke(width, cap = StrokeCap.Round))
}

/** One line per good habit: it fills with green as you tap +, like its full card. */
@Composable
private fun TodayRow(habit: GoodHabitUi, onOpen: () -> Unit, onPlus: () -> Unit, onLongClick: () -> Unit) {
    val s = LocalStrings.current
    val look = rememberGoodFill(habit.progressCount, habit.progressTarget, habit.fillStyle)
    val pulse = rememberCompletionPulse(habit.goalReached)
    NixCard(
        Modifier.fillMaxWidth().graphicsLayer { scaleX = pulse.value; scaleY = pulse.value },
        onClick = onOpen,
        onLongClick = onLongClick,
        paint = look.paint,
        lift = look.background,
        padding = PaddingValues(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        shapeRadius = 18.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(habit.name.forDisplay(), style = NixType.BodyStrong, color = look.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(goodProgressText(habit, s), style = NixType.Small, color = look.contentMuted, maxLines = 1)
            }
            Spacer(Modifier.width(10.dp))
            GoodHabitButton(
                habit.fillStyle == GoodHabitFillStyle.PRAYER, look,
                if (habit.doneToday) NixIcon.Tick else NixIcon.Plus,
                onPlus,
                size = 40.dp,
                tint = if (habit.doneToday) look.background else Nix.colors.onAccent,
                background = if (habit.doneToday) look.content else Nix.colors.accent,
                haptic = true,
            )
        }
    }
}
