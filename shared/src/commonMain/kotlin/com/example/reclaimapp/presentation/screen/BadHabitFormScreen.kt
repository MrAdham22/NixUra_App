@file:OptIn(ExperimentalLayoutApi::class)

package com.example.reclaimapp.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.coach.CoachEngine
import com.example.reclaimapp.domain.recognition.HabitNameHandoff
import com.example.reclaimapp.domain.recognition.HabitPolarity
import com.example.reclaimapp.domain.recognition.HabitRecognition
import com.example.reclaimapp.domain.recognition.HabitRecognizer
import com.example.reclaimapp.presentation.components.ColorPickerRow
import com.example.reclaimapp.presentation.components.ErrorBanner
import com.example.reclaimapp.presentation.components.NixButton
import com.example.reclaimapp.presentation.components.NixButtonStyle
import com.example.reclaimapp.presentation.components.NixCard
import com.example.reclaimapp.presentation.components.NixChip
import com.example.reclaimapp.presentation.components.NixTextField
import com.example.reclaimapp.presentation.components.SectionLabel
import com.example.reclaimapp.presentation.components.ShadePreview
import com.example.reclaimapp.presentation.components.Stepper
import com.example.reclaimapp.presentation.components.SubScreen
import com.example.reclaimapp.presentation.habits.HabitsViewModel
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val HOUR_MILLIS = 3_600_000L

@Composable
fun BadHabitFormScreen(editId: Long?, navigator: Navigator) {
    val vm: HabitsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val engine = koinInject<CoachEngine>()
    val recognizer = koinInject<HabitRecognizer>()
    val handoff = koinInject<HabitNameHandoff>()
    val clock = koinInject<Clock>()
    val s = LocalStrings.current
    val c = Nix.colors
    val editing = editId != null

    var name by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf<Long?>(null) }
    var startChoice by rememberSaveable { mutableStateOf(0) }
    var customDays by rememberSaveable { mutableStateOf(0) }
    var customHours by rememberSaveable { mutableStateOf(0) }
    var loadedEdit by rememberSaveable { mutableStateOf(false) }

    // A name carried over from the Build form ("Track it as a quit habit instead").
    LaunchedEffect(Unit) {
        if (!editing) handoff.take()?.let { name = it }
    }

    LaunchedEffect(editId, state.habits) {
        if (editId != null && !loadedEdit) {
            state.habits.firstOrNull { it.habitId == editId }?.let {
                name = it.name
                color = it.colorArgb
                loadedEdit = true
            }
        }
    }

    // Start options: hours ago
    val presets = listOf(
        s.startNow to 0L,
        s.startHourAgo to 1L,
        s.startYesterday to 24L,
        s.startThreeDays to 72L,
        s.startWeek to 168L,
    )
    val hoursAgo = if (startChoice < presets.size) presets[startChoice].second else customDays * 24L + customHours

    SubScreen(title = if (editing) s.editHabit else s.newBadHabit, onBack = { navigator.back() }) {
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }

        NixTextField(
            value = name,
            onValueChange = { name = it },
            label = s.habitNameLabel,
            placeholder = s.habitNamePlaceholder,
            maxLength = 80,
        )

        if (name.isNotBlank()) {
            val recognition = remember(name) { recognizer.recognize(name) }
            if (!editing) {
                RecognitionHints(
                    recognition = recognition,
                    formPolarity = HabitPolarity.BAD,
                    switchBanner = s.looksGoodBanner,
                    switchLabel = s.moveToBuild,
                    onSwitch = {
                        handoff.put(name)
                        navigator.back()
                        navigator.push(Route.GoodHabitForm())
                    },
                    onApplyCorrection = { name = it },
                )
            }
            val category = recognition.category ?: engine.categoryFor(name)
            NixCard(color = c.accent.copy(alpha = 0.10f), borderColor = c.accent.copy(alpha = 0.35f)) {
                Text(s.coachUnderstood.uppercase(), style = NixType.Eyebrow, color = c.accent)
                Spacer(Modifier.height(6.dp))
                Text(category.label, style = NixType.Heading, color = c.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(category.whyItMatters, style = NixType.Body, color = c.textSecondary)
                category.milestones.firstOrNull()?.let {
                    Spacer(Modifier.height(10.dp))
                    Text("${s.firstMilestone}: ${it.title}", style = NixType.SmallStrong, color = c.textSecondary)
                }
                category.safetyNote?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, style = NixType.Small, color = c.accentWarm)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(s.cardColor)
            ColorPickerRow(selected = color, onSelect = { color = it })
            ShadePreview(color)
            Text(s.cardColorHint, style = NixType.Small, color = c.textMuted)
        }

        if (!editing) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(s.lastTimeLabel)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEachIndexed { index, (label, _) ->
                        NixChip(label, selected = startChoice == index, onClick = { startChoice = index })
                    }
                    NixChip(s.startCustom, selected = startChoice == presets.size, onClick = { startChoice = presets.size })
                }
                if (startChoice == presets.size) {
                    NixCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.unitDays, style = NixType.BodyStrong, color = c.textSecondary, modifier = Modifier.weight(1f))
                            Stepper(customDays, { customDays = it }, max = 3650)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.unitHours, style = NixType.BodyStrong, color = c.textSecondary, modifier = Modifier.weight(1f))
                            Stepper(customHours, { customHours = it }, max = 23)
                        }
                    }
                }
                Text(s.lastTimeHint, style = NixType.Small, color = c.textMuted)
            }
        }

        NixButton(
            text = if (editing) s.saveChanges else s.startTracking,
            onClick = {
                if (editId != null) {
                    vm.onUpdateHabit(editId, name, color) { navigator.back() }
                } else {
                    val start = clock.nowEpochMillis() - hoursAgo * HOUR_MILLIS
                    vm.onCreateHabit(name, color, start) { navigator.back() }
                }
            },
            enabled = name.isNotBlank(),
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Confidence needed before a form offers to move the habit to the other list. */
private const val SWITCH_CONFIDENCE = 0.7f

/**
 * Non-blocking hints under the name field: "this looks like a good/bad habit" with a switch button, and a
 * "Did you mean ...?" chip for typos. Never prevents saving.
 */
@Composable
internal fun RecognitionHints(
    recognition: HabitRecognition,
    formPolarity: HabitPolarity,
    switchBanner: String,
    switchLabel: String,
    onSwitch: () -> Unit,
    onApplyCorrection: (String) -> Unit,
) {
    val s = LocalStrings.current
    val c = Nix.colors
    val suggestsOtherList = recognition.polarity != HabitPolarity.UNKNOWN &&
        recognition.polarity != formPolarity &&
        recognition.polarityConfidence >= SWITCH_CONFIDENCE
    val fixedText = recognition.correctedText
    val fixedWord = recognition.suggestedCorrection

    if (fixedText != null && fixedWord != null) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NixChip(s.didYouMean(fixedWord), selected = false, onClick = { onApplyCorrection(fixedText) })
        }
    }
    if (suggestsOtherList) {
        NixCard(color = c.accentWarm.copy(alpha = 0.10f), borderColor = c.accentWarm.copy(alpha = 0.35f)) {
            Text(switchBanner, style = NixType.Body, color = c.textPrimary)
            Spacer(Modifier.height(10.dp))
            NixButton(
                text = switchLabel,
                onClick = onSwitch,
                style = NixButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
