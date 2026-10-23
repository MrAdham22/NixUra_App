package com.example.reclaimapp.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.presentation.coach.CoachViewModel
import com.example.reclaimapp.presentation.components.BadHabitCard
import com.example.reclaimapp.presentation.components.EmptyState
import com.example.reclaimapp.presentation.components.EventCard
import com.example.reclaimapp.presentation.components.GoodHabitCard
import com.example.reclaimapp.presentation.components.MomentumBars
import com.example.reclaimapp.presentation.components.NixButton
import com.example.reclaimapp.presentation.components.NixButtonStyle
import com.example.reclaimapp.presentation.components.NixCard
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconView
import com.example.reclaimapp.presentation.components.SectionLabel
import com.example.reclaimapp.presentation.components.TabScreen
import com.example.reclaimapp.presentation.events.EventsViewModel
import com.example.reclaimapp.presentation.goodhabits.GoodHabitsViewModel
import com.example.reclaimapp.presentation.habits.HabitsViewModel
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.profile.ProfileViewModel
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import com.example.reclaimapp.presentation.theme.NixUraPalette
import com.example.reclaimapp.presentation.util.formatDateTime
import com.example.reclaimapp.presentation.util.rememberNowMillis
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

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
    val now by rememberNowMillis()
    val days = koinInject<DayCalculator>()
    val s = LocalStrings.current
    val c = Nix.colors

    val hour = (days.millisOfDay(now) / 3_600_000L).toInt()
    val greeting = when {
        hour < 12 -> s.greetingMorning
        hour < 18 -> s.greetingAfternoon
        else -> s.greetingEvening
    }
    val nextEvent = events.events.firstOrNull { it.targetAtEpochMillis > now }
    val stats = profile.stats

    TabScreen {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s.homeEyebrow.uppercase(), style = NixType.Eyebrow, color = c.accent)
            Text(
                if (profile.name.isBlank()) greeting else "$greeting, ${profile.name}",
                style = NixType.Display,
                color = c.textPrimary,
            )
        }

        // Focus card (offline coach)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(NixUraPalette.Purple, NixUraPalette.Plum)))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NixIconView(NixIcon.Sparkle, NixUraPalette.Orange, size = 18.dp)
                Spacer(Modifier.width(8.dp))
                Text(s.todaysFocus.uppercase(), style = NixType.Eyebrow, color = NixUraPalette.Blush.copy(alpha = 0.85f))
            }
            Text(
                coach.insight?.body ?: s.focusEmpty,
                style = NixType.Heading,
                color = NixUraPalette.Blush,
            )
            NixButton(s.openCoach, { navigator.selectTab(Route.Coach) }, style = NixButtonStyle.Secondary, icon = NixIcon.Sparkle)
        }

        // Momentum
        NixCard {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(s.momentum.uppercase(), style = NixType.Eyebrow, color = c.textMuted)
                    Spacer(Modifier.height(6.dp))
                    Text(stats.momentum?.let { "$it%" } ?: "—", style = NixType.counter(40), color = c.textPrimary)
                    Text(
                        when {
                            stats.momentum == null -> s.momentumEmpty
                            stats.disciplineDelta != null -> s.momentumDelta(stats.disciplineDelta)
                            else -> s.momentumAcross
                        },
                        style = NixType.Small,
                        color = c.textSecondary,
                    )
                }
                if (stats.weekBars.isNotEmpty()) {
                    MomentumBars(stats.weekBars, Modifier.width(150.dp))
                }
            }
        }

        // Quitting
        SectionLabel(s.quittingSection) {
            Text(s.seeAll, style = NixType.SmallStrong, color = c.accent, modifier = Modifier.clickableNoRipple { navigator.selectTab(Route.BadHabits) })
        }
        if (habits.habits.isEmpty()) {
            EmptyState(NixIcon.Shield, s.badEmptyTitle, s.badEmptyBody, actionText = s.badAdd, onAction = { navigator.push(Route.BadHabitForm()) })
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                habits.habits.take(2).forEach { BadHabitCard(it, onClick = { navigator.push(Route.BadHabitDetail(it.habitId)) }) }
            }
        }

        // Building
        SectionLabel(s.buildingSection) {
            Text(s.seeAll, style = NixType.SmallStrong, color = c.accent, modifier = Modifier.clickableNoRipple { navigator.selectTab(Route.GoodHabits) })
        }
        if (good.habits.isEmpty()) {
            EmptyState(NixIcon.CircleCheck, s.goodEmptyTitle, s.goodEmptyBody, actionText = s.goodAdd, onAction = { navigator.push(Route.GoodHabitForm()) })
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                good.habits.take(2).forEach { habit ->
                    GoodHabitCard(
                        habit, good.todayIndex,
                        onClick = { navigator.push(Route.GoodHabitDetail(habit.id)) },
                        onPlus = { goodVm.onChangeToday(habit.id, +1) },
                        onMinus = { goodVm.onChangeToday(habit.id, -1) },
                    )
                }
            }
        }

        // Next event
        SectionLabel(s.nextEventSection) {
            Text(s.seeAll, style = NixType.SmallStrong, color = c.accent, modifier = Modifier.clickableNoRipple { navigator.selectTab(Route.Events) })
        }
        if (nextEvent == null) {
            EmptyState(NixIcon.Calendar, s.eventsEmptyTitle, s.eventsEmptyBody, actionText = s.eventsAdd, onAction = { navigator.push(Route.EventForm()) })
        } else {
            EventCard(nextEvent, now, formatDateTime(nextEvent.targetAtEpochMillis, days, s), onClick = { navigator.push(Route.EventForm(nextEvent.id)) })
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    ),
)
