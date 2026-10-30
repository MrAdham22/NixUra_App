@file:OptIn(ExperimentalLayoutApi::class)

package com.example.reclaimapp.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.domain.recognition.HabitNameHandoff
import com.example.reclaimapp.domain.recognition.HabitPolarity
import com.example.reclaimapp.domain.recognition.HabitRecognizer
import com.example.reclaimapp.presentation.components.ErrorBanner
import com.example.reclaimapp.presentation.components.NixButton
import com.example.reclaimapp.presentation.components.NixCard
import com.example.reclaimapp.presentation.components.NixChip
import com.example.reclaimapp.presentation.components.NixTextField
import com.example.reclaimapp.presentation.components.SectionLabel
import com.example.reclaimapp.presentation.components.Stepper
import com.example.reclaimapp.presentation.components.SubScreen
import com.example.reclaimapp.presentation.goodhabits.GoodHabitsViewModel
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GoodHabitFormScreen(editId: Long?, navigator: Navigator) {
    val vm: GoodHabitsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val recognizer = koinInject<HabitRecognizer>()
    val handoff = koinInject<HabitNameHandoff>()
    val s = LocalStrings.current
    val c = Nix.colors
    val editing = editId != null

    var name by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf(1) }
    var loadedEdit by rememberSaveable { mutableStateOf(false) }

    // A name carried over from the Quit form ("Add it to Build instead").
    LaunchedEffect(Unit) {
        if (!editing) handoff.take()?.let { name = it }
    }

    LaunchedEffect(editId, state.habits) {
        if (editId != null && !loadedEdit) {
            state.habits.firstOrNull { it.id == editId }?.let {
                name = it.name
                target = it.dailyTarget
                loadedEdit = true
            }
        }
    }

    SubScreen(title = if (editing) s.editHabit else s.newGoodHabit, onBack = { navigator.back() }) {
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }

        NixTextField(
            value = name,
            onValueChange = { name = it },
            label = s.habitNameLabel,
            placeholder = s.goodNamePlaceholder,
            maxLength = 80,
        )

        if (!editing && name.isNotBlank()) {
            val recognition = remember(name) { recognizer.recognize(name) }
            RecognitionHints(
                recognition = recognition,
                formPolarity = HabitPolarity.GOOD,
                switchBanner = s.looksBadBanner,
                switchLabel = s.moveToQuit,
                onSwitch = {
                    handoff.put(name)
                    navigator.back()
                    navigator.push(Route.BadHabitForm())
                },
                onApplyCorrection = { name = it },
            )
        }

        if (!editing) {
            SectionLabel(s.quickPicks)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.goodSuggestions.forEach { (label, suggestedTarget) ->
                    NixChip(label, selected = name == label, onClick = { name = label; target = suggestedTarget })
                }
            }
        }

        NixCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    Text(s.dailyTarget, style = NixType.BodyStrong, color = c.textPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(s.dailyTargetHint, style = NixType.Small, color = c.textMuted)
                }
                Stepper(target, { target = it }, min = 1, max = 99)
            }
        }

        NixButton(
            text = if (editing) s.saveChanges else s.addHabit,
            onClick = {
                if (editId != null) vm.onUpdate(editId, name, target, null) { navigator.back() }
                else vm.onCreate(name, target, null) { navigator.back() }
            },
            enabled = name.isNotBlank(),
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
