package com.adhamelgendy.nixura.presentation.screen

import com.adhamelgendy.nixura.domain.usecase.DeletableItem
import com.adhamelgendy.nixura.presentation.deletion.rememberDeleteFlow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.adhamelgendy.nixura.presentation.guide.GuideTarget
import com.adhamelgendy.nixura.presentation.guide.guideTarget
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.presentation.components.EmptyState
import com.adhamelgendy.nixura.presentation.components.ErrorBanner
import com.adhamelgendy.nixura.presentation.components.EventCard
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconButton
import com.adhamelgendy.nixura.presentation.components.ScreenHeader
import com.adhamelgendy.nixura.presentation.components.SectionLabel
import com.adhamelgendy.nixura.presentation.components.TabScreen
import com.adhamelgendy.nixura.presentation.events.EventsViewModel
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.util.formatDateTime
import com.adhamelgendy.nixura.presentation.util.rememberNowMillis
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EventsScreen(navigator: Navigator) {
    val vm: EventsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val now by rememberNowMillis()
    val days = koinInject<DayCalculator>()
    val s = LocalStrings.current
    val c = Nix.colors
    val deleteFlow = rememberDeleteFlow()

    val upcoming = state.events.filter { it.targetAtEpochMillis > now }
    val past = state.events.filter { it.targetAtEpochMillis <= now }.sortedByDescending { it.targetAtEpochMillis }

    TabScreen {
        ScreenHeader(
            eyebrow = s.eventsEyebrow,
            title = s.eventsTitle,
            subtitle = s.eventsSubtitle,
            trailing = {
                NixIconButton(
                    NixIcon.Plus,
                    { navigator.push(Route.EventForm()) },
                    modifier = Modifier.guideTarget(GuideTarget.AddEvent),
                    tint = c.onAccent,
                    background = c.accent,
                    bordered = false,
                )
            },
        )
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }

        if (!state.isLoading && state.events.isEmpty()) {
            EmptyState(
                icon = NixIcon.Calendar,
                title = s.eventsEmptyTitle,
                body = s.eventsEmptyBody,
                actionText = s.eventsAdd,
                onAction = { navigator.push(Route.EventForm()) },
            )
        }
        if (upcoming.isNotEmpty()) {
            SectionLabel(s.upcoming)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                upcoming.forEach { event ->
                    EventCard(
                        event, now, formatDateTime(event.targetAtEpochMillis, days, s),
                        onClick = { navigator.push(Route.EventForm(event.id)) },
                        onLongClick = { deleteFlow.open(DeletableItem.Event(event.id), event.title) },
                    )
                }
            }
        }
        if (past.isNotEmpty()) {
            SectionLabel(s.pastEvents)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                past.forEach { event ->
                    EventCard(
                        event, now, formatDateTime(event.targetAtEpochMillis, days, s),
                        onClick = { navigator.push(Route.EventForm(event.id)) },
                        onLongClick = { deleteFlow.open(DeletableItem.Event(event.id), event.title) },
                    )
                }
            }
        }
    }
}
