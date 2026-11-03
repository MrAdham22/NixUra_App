package com.example.reclaimapp.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.reclaimapp.presentation.components.InfoSheet
import com.example.reclaimapp.presentation.guide.GuideTarget
import com.example.reclaimapp.presentation.guide.guideTarget
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.presentation.components.BadHabitCard
import com.example.reclaimapp.presentation.components.EmptyState
import com.example.reclaimapp.presentation.components.ErrorBanner
import com.example.reclaimapp.presentation.components.NixCard
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconButton
import com.example.reclaimapp.presentation.components.ScreenHeader
import com.example.reclaimapp.presentation.components.TabScreen
import com.example.reclaimapp.presentation.habits.HabitsViewModel
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import com.example.reclaimapp.presentation.util.formatElapsed
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BadHabitsScreen(navigator: Navigator) {
    val vm: HabitsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val c = Nix.colors
    var bestInfo by remember { mutableStateOf(false) }

    TabScreen {
        ScreenHeader(
            eyebrow = s.badEyebrow,
            title = s.badTitle,
            subtitle = s.badSubtitle,
            trailing = {
                NixIconButton(
                    NixIcon.Plus,
                    { navigator.push(Route.BadHabitForm()) },
                    modifier = Modifier.guideTarget(GuideTarget.AddQuit),
                    tint = c.onAccent,
                    background = c.accent,
                    bordered = false,
                )
            },
        )
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }

        if (!state.isLoading && state.habits.isEmpty()) {
            EmptyState(
                icon = NixIcon.Shield,
                title = s.badEmptyTitle,
                body = s.badEmptyBody,
                actionText = s.badAdd,
                onAction = { navigator.push(Route.BadHabitForm()) },
            )
        } else if (state.habits.isNotEmpty()) {
            val best = state.habits.maxByOrNull { it.activeElapsed }
            NixCard(onClick = { bestInfo = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.statHabitsQuitting.uppercase(), style = NixType.Eyebrow, color = c.textMuted)
                        Spacer(Modifier.height(4.dp))
                        Text(state.habits.size.toString(), style = NixType.counter(28), color = c.textPrimary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(2f)) {
                        Text(s.statBestActive.uppercase(), style = NixType.Eyebrow, color = c.textMuted)
                        Spacer(Modifier.height(4.dp))
                        Text(best?.let { formatElapsed(it.activeElapsed) } ?: "—", style = NixType.counter(28), color = c.textPrimary)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                state.habits.forEach { habit ->
                    BadHabitCard(habit, onClick = { navigator.push(Route.BadHabitDetail(habit.habitId)) })
                }
            }
        }
    }
    if (bestInfo) {
        val best = state.habits.maxByOrNull { it.activeElapsed }
        InfoSheet(s.statBestActive, best?.let { formatElapsed(it.activeElapsed) }, s.infoBestActive, onDismiss = { bestInfo = false })
    }
}
