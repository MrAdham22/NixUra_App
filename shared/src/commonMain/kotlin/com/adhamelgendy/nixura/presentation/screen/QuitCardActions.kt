package com.adhamelgendy.nixura.presentation.screen

import com.adhamelgendy.nixura.domain.widget.WidgetKind
import com.adhamelgendy.nixura.presentation.components.rememberAddToHomeAction
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

/** What a quit card can do from a list: its options sheet (with "Hide name outside the app") and the craving shortcut. */
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
    var hideFor by remember { mutableStateOf<HabitCardUiModel?>(null) }
    val addToHome = rememberAddToHomeAction()

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

    // Hiding says what it does and what it cannot do first; showing the name again just happens.
    hideFor?.let { habit ->
        ConfirmDialog(
            title = s.hideNameTitle,
            message = "${s.hideNameHint}\n\n${s.hideNameLimits}",
            confirmText = s.hideNameConfirm,
            cancelText = s.cancel,
            onConfirm = {
                habitsVm.onHideNameOutside(habit.habitId, true)
                hideFor = null
            },
            onDismiss = { hideFor = null },
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
                    SheetAction(s.hideNameSheetRow(habit.hideNameOutside), if (habit.hideNameOutside) NixIcon.EyeOff else NixIcon.Eye) {
                        if (habit.hideNameOutside) habitsVm.onHideNameOutside(habit.habitId, false) else hideFor = habit
                    },
                ) + listOfNotNull(addToHome?.let { pin -> SheetAction(s.widgetAddToHome, NixIcon.Home) { pin(WidgetKind.QUIT_COUNTER, habit.habitId) } }),
            )
        },
        craving = { habit ->
            coachVm.onCraving(habit.habitId, s.cravingPrompt(habit.name))
            navigator.selectTab(Route.Coach)
        },
    )
}
