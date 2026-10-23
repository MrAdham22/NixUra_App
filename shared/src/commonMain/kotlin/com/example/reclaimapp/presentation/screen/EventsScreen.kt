package com.example.reclaimapp.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.presentation.components.EmptyState
import com.example.reclaimapp.presentation.components.ErrorBanner
import com.example.reclaimapp.presentation.components.EventCard
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconButton
import com.example.reclaimapp.presentation.components.ScreenHeader
import com.example.reclaimapp.presentation.components.SectionLabel
import com.example.reclaimapp.presentation.components.TabScreen
import com.example.reclaimapp.presentation.events.EventsViewModel
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.util.formatDateTime
import com.example.reclaimapp.presentation.util.rememberNowMillis
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
                    EventCard(event, now, formatDateTime(event.targetAtEpochMillis, days, s), onClick = { navigator.push(Route.EventForm(event.id)) })
                }
            }
        }
        if (past.isNotEmpty()) {
            SectionLabel(s.pastEvents)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                past.forEach { event ->
                    EventCard(event, now, formatDateTime(event.targetAtEpochMillis, days, s), onClick = { navigator.push(Route.EventForm(event.id)) }, modifier = Modifier)
                }
            }
        }
    }
}
