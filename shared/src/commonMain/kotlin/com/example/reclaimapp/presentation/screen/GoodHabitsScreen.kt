package com.example.reclaimapp.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.presentation.components.EmptyState
import com.example.reclaimapp.presentation.components.ErrorBanner
import com.example.reclaimapp.presentation.components.GoodHabitCard
import com.example.reclaimapp.presentation.components.NixCard
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconButton
import com.example.reclaimapp.presentation.components.NixProgressBar
import com.example.reclaimapp.presentation.components.ScreenHeader
import com.example.reclaimapp.presentation.components.TabScreen
import com.example.reclaimapp.presentation.goodhabits.GoodHabitsViewModel
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GoodHabitsScreen(navigator: Navigator) {
    val vm: GoodHabitsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val c = Nix.colors

    TabScreen {
        ScreenHeader(
            eyebrow = s.goodEyebrow,
            title = s.goodTitle,
            subtitle = s.goodSubtitle,
            trailing = {
                NixIconButton(
                    NixIcon.Plus,
                    { navigator.push(Route.GoodHabitForm()) },
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
            val done = state.habits.count { it.doneToday }
            NixCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.todayLabel.uppercase(), style = NixType.Eyebrow, color = c.textMuted, modifier = Modifier.weight(1f))
                    Text(s.habitsDoneToday(done, state.habits.size), style = NixType.SmallStrong, color = c.textSecondary)
                }
                Spacer(Modifier.height(10.dp))
                NixProgressBar(done.toFloat() / state.habits.size, color = c.positive)
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                state.habits.forEach { habit ->
                    GoodHabitCard(
                        habit = habit,
                        todayIndex = state.todayIndex,
                        onClick = { navigator.push(Route.GoodHabitDetail(habit.id)) },
                        onPlus = { vm.onChangeToday(habit.id, +1) },
                        onMinus = { vm.onChangeToday(habit.id, -1) },
                    )
                }
            }
        }
    }
}
