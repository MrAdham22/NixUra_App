package com.adhamelgendy.nixura.presentation.screen

import com.adhamelgendy.nixura.domain.usecase.DeletableItem
import com.adhamelgendy.nixura.presentation.deletion.rememberDeleteFlow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.adhamelgendy.nixura.presentation.guide.GuideTarget
import com.adhamelgendy.nixura.presentation.guide.guideTarget
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.presentation.components.EmptyState
import com.adhamelgendy.nixura.presentation.components.ErrorBanner
import com.adhamelgendy.nixura.presentation.components.GoodHabitCard
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconButton
import com.adhamelgendy.nixura.presentation.components.NixProgressBar
import com.adhamelgendy.nixura.presentation.components.ScreenHeader
import com.adhamelgendy.nixura.presentation.components.TabScreen
import com.adhamelgendy.nixura.presentation.goodhabits.GoodHabitsViewModel
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GoodHabitsScreen(navigator: Navigator) {
    val vm: GoodHabitsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val c = Nix.colors
    val deleteFlow = rememberDeleteFlow()

    TabScreen {
        ScreenHeader(
            eyebrow = s.goodEyebrow,
            title = s.goodTitle,
            subtitle = s.goodSubtitle,
            trailing = {
                NixIconButton(
                    NixIcon.Plus,
                    { navigator.push(Route.GoodHabitForm()) },
                    modifier = Modifier.guideTarget(GuideTarget.AddGood),
                    tint = c.onAccent,
                    background = c.accent,
                    bordered = false,
                )
            },
        )
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }

        if (!state.isLoading && state.habits.isEmpty()) {
            EmptyState(
                icon = NixIcon.CircleCheck,
                title = s.goodEmptyTitle,
                body = s.goodEmptyBody,
                actionText = s.goodAdd,
                onAction = { navigator.push(Route.GoodHabitForm()) },
            )
        } else if (state.habits.isNotEmpty()) {
            // A weekly habit whose goal is met needs nothing more today.
            val done = state.habits.count { it.settledToday }
            NixCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.todayLabel.uppercase(), style = NixType.Eyebrow, color = c.textMuted, modifier = Modifier.weight(1f))
                    Text(s.habitsDoneToday(done, state.habits.size), style = NixType.SmallStrong, color = c.textSecondary)
                }
                Spacer(Modifier.height(10.dp))
                // Partial progress counts too: a prayer card at 4 of 5 fills most of its share of the bar.
                val progress = state.habits.sumOf { h ->
                    if (h.settledToday) 1.0 else (h.progressCount.toDouble() / h.progressTarget.coerceAtLeast(1)).coerceIn(0.0, 1.0)
                } / state.habits.size
                NixProgressBar(progress.toFloat(), color = c.positive)
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                state.habits.forEach { habit ->
                    GoodHabitCard(
                        habit = habit,
                        todayIndex = state.todayIndex,
                        onClick = { navigator.push(Route.GoodHabitDetail(habit.id)) },
                        onPlus = { vm.onChangeToday(habit.id, +1) },
                        onMinus = { vm.onChangeToday(habit.id, -1) },
                        onLongClick = { deleteFlow.open(DeletableItem.GoodHabit(habit.id), habit.name) },
                    )
                }
            }
        }
    }
}
