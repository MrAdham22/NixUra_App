@file:OptIn(ExperimentalLayoutApi::class)

package com.adhamelgendy.nixura.presentation.screen

import com.adhamelgendy.nixura.presentation.util.forDisplay
import com.adhamelgendy.nixura.presentation.theme.ProvideNixColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.calculator.WeeklyGoalCalculator
import com.adhamelgendy.nixura.domain.recognition.HabitNameHandoff
import com.adhamelgendy.nixura.domain.recognition.HabitPolarity
import com.adhamelgendy.nixura.domain.recognition.HabitRecognizer
import com.adhamelgendy.nixura.domain.recognition.PrayerHabitDetector
import com.adhamelgendy.nixura.presentation.components.PrayerGoldWash
import com.adhamelgendy.nixura.presentation.components.PrayerWordTransformation
import com.adhamelgendy.nixura.presentation.theme.PrayerPalette
import com.adhamelgendy.nixura.presentation.components.ErrorBanner
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.PrayerGoldButton
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixChip
import com.adhamelgendy.nixura.presentation.components.NixTextField
import com.adhamelgendy.nixura.presentation.components.SectionLabel
import com.adhamelgendy.nixura.presentation.components.Stepper
import com.adhamelgendy.nixura.presentation.components.SubScreen
import com.adhamelgendy.nixura.presentation.goodhabits.GoodHabitsViewModel
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
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
    var weekly by rememberSaveable { mutableStateOf(false) }
    var daysPerWeek by rememberSaveable { mutableStateOf(DEFAULT_DAYS_PER_WEEK) }
    var loadedEdit by rememberSaveable { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }

    // A prayer habit ("pray", "Salah", "صلاة"...): five a day, and the whole page turns gold from the word down.
    val prayer = remember(name) { PrayerHabitDetector.isPrayer(name) }
    var prayerApplied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(prayer) {
        if (prayer && !prayerApplied && !editing) {
            target = PrayerHabitDetector.DAILY_PRAYERS
            weekly = false
        }
        prayerApplied = prayer
    }

    // A name carried over from the Quit form ("Add it to Build instead").
    LaunchedEffect(Unit) {
        if (!editing) handoff.take()?.let { name = it }
    }

    LaunchedEffect(editId, state.habits) {
        if (editId != null && !loadedEdit) {
            state.habits.firstOrNull { it.id == editId }?.let {
                name = it.name
                target = it.dailyTarget
                weekly = it.isWeekly
                it.weeklyTarget?.let { days -> daysPerWeek = days }
                loadedEdit = true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    PrayerGoldWash(prayer)
    // In the prayer look, selections (chips, "Every day", the stepper, the cursor) are gold like the gold button,
    // instead of the app's rose: one world on the page.
    val base = Nix.colors
    ProvideNixColors(if (prayer) base.copy(accent = PrayerPalette.Gold, onAccent = PrayerPalette.Night) else base) {
    SubScreen(title = if (editing) s.editHabit else s.newGoodHabit, onBack = { navigator.back() }) {
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }

        NixTextField(
            value = name,
            onValueChange = { name = it },
            label = s.goodHabitNameLabel,
            placeholder = s.goodNamePlaceholder,
            maxLength = 80,
            focusRequester = nameFocus,
            visualTransformation = if (prayer) PrayerWordTransformation(PrayerPalette.Gold) else VisualTransformation.None,
        )
        if (prayer) Text(s.prayerHint, style = NixType.Small, color = PrayerPalette.GoldLight)
        // Same name (or a second prayer habit): say so, the user may have meant the one they already have.
        val existing = if (editing) null else state.habits.firstOrNull { it.isSameHabitAs(name) }
        if (existing != null) {
            Text(s.alreadyTracking(existing.name.forDisplay()), style = NixType.SmallStrong, color = if (prayer) PrayerPalette.Cream else c.accentWarm)
        }

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
            SectionLabel(s.quickPicks, color = if (prayer) PrayerPalette.GoldLight else c.textMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NixChip(
                    s.customHabitPill,
                    selected = name.isNotBlank() && s.goodSuggestions.none { it.first == name },
                    onClick = {
                        if (s.goodSuggestions.any { it.first == name }) name = ""
                        nameFocus.requestFocus()
                    },
                )
                // Picks the user already tracks are left out.
                s.goodSuggestions.filter { (label, _) -> state.habits.none { it.isSameHabitAs(label) } }.forEach { (label, suggestedTarget) ->
                    NixChip(label, selected = name == label, onClick = { name = label; target = suggestedTarget; weekly = false })
                }
            }
        }

        NixCard {
            Text(s.habitFrequency, style = NixType.BodyStrong, color = c.textPrimary)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NixChip(s.frequencyDaily, selected = !weekly, onClick = { weekly = false })
                NixChip(s.frequencyWeekly, selected = weekly, onClick = { weekly = true })
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    Text(if (weekly) s.weeklyTarget else s.dailyTarget, style = NixType.BodyStrong, color = c.textPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(if (weekly) s.weeklyTargetHint else s.dailyTargetHint, style = NixType.Small, color = c.textMuted)
                }
                if (weekly) {
                    Stepper(daysPerWeek, { daysPerWeek = it }, min = 1, max = WeeklyGoalCalculator.MAX_DAYS_PER_WEEK)
                } else {
                    Stepper(target, { target = it }, min = 1, max = 99)
                }
            }
        }

        val save = {
            val weeklyTarget = if (weekly) daysPerWeek else null
            if (editId != null) vm.onUpdate(editId, name, target, null, weeklyTarget) { navigator.back() }
            else vm.onCreate(name, target, null, weeklyTarget) { navigator.back() }
        }
        if (prayer) {
            PrayerGoldButton(
                text = if (editing) s.saveChanges else s.addHabit,
                onClick = save,
                enabled = name.isNotBlank(),
                loading = state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            NixButton(
                text = if (editing) s.saveChanges else s.addHabit,
                onClick = save,
                enabled = name.isNotBlank(),
                loading = state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    }
    }
}

private const val DEFAULT_DAYS_PER_WEEK = 3

/** True when [typed] names this habit: the same words ignoring case and spaces, or both are prayer habits. */
private fun com.adhamelgendy.nixura.presentation.goodhabits.GoodHabitUi.isSameHabitAs(typed: String): Boolean {
    val a = name.trim().lowercase()
    val b = typed.trim().lowercase()
    if (b.isEmpty()) return false
    return a == b || (PrayerHabitDetector.isPrayer(name) && PrayerHabitDetector.isPrayer(typed))
}
