package com.adhamelgendy.nixura.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.adhamelgendy.nixura.domain.usecase.DeletableItem
import com.adhamelgendy.nixura.presentation.coach.CoachViewModel
import com.adhamelgendy.nixura.presentation.components.ConfirmDialog
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.deletion.DeleteFlow
import com.adhamelgendy.nixura.presentation.deletion.SheetAction
import com.adhamelgendy.nixura.presentation.habits.HabitCardUiModel
import com.adhamelgendy.nixura.presentation.habits.HabitsViewModel
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import org.koin.compose.viewmodel.koinViewModel

/** What a quit card can do from a list: its options sheet and the craving shortcut. */
class QuitCardActions(
    val openOptions: (HabitCardUiModel) -> Unit,
    val craving: (HabitCardUiModel) -> Unit,
)

/**
 * Quit-card actions shared by Home and the Quit tab: the options sheet (I slipped, Edit habit, Delete), the slip
 * confirmation, and "Craving?", which opens the coach already talking about that habit.
 */
@Composable
fun rememberQuitCardActions(navigator: Navigator, deleteFlow: DeleteFlow): QuitCardActions {
    val s = LocalStrings.current
    val habitsVm: HabitsViewModel = koinViewModel()
    // The same coach instance the Coach tab shows (one view model per activity).
    val coachVm: CoachViewModel = koinViewModel()
    var slipFor by remember { mutableStateOf<HabitCardUiModel?>(null) }

    slipFor?.let { habit ->
        ConfirmDialog(
            title = s.relapseConfirmTitle,
            message = s.relapseConfirmBody,
            confirmText = s.recordRelapse,
            cancelText = s.cancel,
            danger = true,
            onConfirm = {
                habitsVm.onRecordRelapse(habit.habitId)
                slipFor = null
            },
            onDismiss = { slipFor = null },
        )
    }

    return QuitCardActions(
        openOptions = { habit ->
            deleteFlow.open(
                DeletableItem.BadHabit(habit.habitId),
                habit.name,
                listOf(
                    SheetAction(s.slipAction, NixIcon.Reset) { slipFor = habit },
                    SheetAction(s.editHabit, NixIcon.Pencil) { navigator.push(Route.BadHabitForm(habit.habitId)) },
                ),
            )
        },
        craving = { habit ->
            coachVm.onCraving(habit.habitId, s.cravingPrompt(habit.name))
            navigator.selectTab(Route.Coach)
        },
    )
}
