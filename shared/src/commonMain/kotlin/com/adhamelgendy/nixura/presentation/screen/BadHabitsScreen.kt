package com.adhamelgendy.nixura.presentation.screen

import com.adhamelgendy.nixura.domain.usecase.DeletableItem
import com.adhamelgendy.nixura.presentation.deletion.rememberDeleteFlow
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
import com.adhamelgendy.nixura.presentation.components.InfoSheet
import com.adhamelgendy.nixura.presentation.guide.GuideTarget
import com.adhamelgendy.nixura.presentation.guide.guideTarget
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.presentation.components.BadHabitCard
import com.adhamelgendy.nixura.presentation.components.EmptyState
import com.adhamelgendy.nixura.presentation.components.ErrorBanner
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconButton
import com.adhamelgendy.nixura.presentation.components.ScreenHeader
import com.adhamelgendy.nixura.presentation.components.TabScreen
import com.adhamelgendy.nixura.presentation.habits.HabitsViewModel
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.util.formatElapsed
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BadHabitsScreen(navigator: Navigator) {
    val vm: HabitsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val c = Nix.colors
    var bestInfo by remember { mutableStateOf(false) }
    val deleteFlow = rememberDeleteFlow()

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
                    BadHabitCard(
                        habit,
                        onClick = { navigator.push(Route.BadHabitDetail(habit.habitId)) },
                        onLongClick = { deleteFlow.open(DeletableItem.BadHabit(habit.habitId), habit.name) },
                    )
                }
            }
        }
    }
    if (bestInfo) {
        val best = state.habits.maxByOrNull { it.activeElapsed }
        InfoSheet(s.statBestActive, best?.let { formatElapsed(it.activeElapsed) }, s.infoBestActive, onDismiss = { bestInfo = false })
    }
}
