@file:OptIn(ExperimentalLayoutApi::class)

package com.adhamelgendy.nixura.presentation.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.domain.calculator.WeeklyGoalCalculator
import com.adhamelgendy.nixura.domain.coach.ai.CoachAction
import com.adhamelgendy.nixura.domain.coach.ai.CoachActionProblem
import com.adhamelgendy.nixura.domain.coach.ai.CoachActionRules
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.usecase.MAX_DAILY_TARGET
import com.adhamelgendy.nixura.domain.usecase.MAX_EVENT_TITLE
import com.adhamelgendy.nixura.presentation.components.DateTimePickerDialog
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixChip
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.components.NixTextField
import com.adhamelgendy.nixura.presentation.components.SectionLabel
import com.adhamelgendy.nixura.presentation.components.Stepper
import com.adhamelgendy.nixura.presentation.strings.Strings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.util.formatDateTime
import org.koin.compose.koinInject

private const val DEFAULT_DAYS_PER_WEEK = 3

/**
 * The edit form for one understood item, built from the same pieces as the add forms.
 * [problemOf] applies the add form rules; "Keep changes" works only when there is no problem.
 */
@Composable
internal fun VoiceItemEditor(
    initial: CoachAction,
    problemOf: (CoachAction) -> CoachActionProblem?,
    onKeep: (CoachAction) -> Unit,
    onCancel: () -> Unit,
    s: Strings,
) {
    val c = Nix.colors
    val days = koinInject<DayCalculator>()
    var draft by remember(initial) { mutableStateOf(initial) }
    var showPicker by remember { mutableStateOf(false) }
    val problem = problemOf(draft)

    Text(s.voiceEditTitle, style = NixType.Heading, color = c.textPrimary)
    Spacer(Modifier.height(4.dp))
    Text(s.voiceEditHint, style = NixType.Small, color = c.textMuted, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (val d = draft) {
            is CoachAction.Quit, is CoachAction.Build -> {
                val quit = d is CoachAction.Quit
                val name = if (d is CoachAction.Quit) d.name else (d as CoachAction.Build).name
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(s.voiceEditKind)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NixChip(s.actionQuit, selected = quit, onClick = { if (!quit) draft = CoachActionRules.switchKind(d) })
                        NixChip(s.actionBuild, selected = !quit, onClick = { if (quit) draft = CoachActionRules.switchKind(d) })
                    }
                }
                // The length limit is checked below, so a too-long name the AI wrote can still be shortened here.
                NixTextField(
                    value = name,
                    onValueChange = { new -> draft = if (d is CoachAction.Quit) d.copy(name = new) else (d as CoachAction.Build).copy(name = new) },
                    label = if (quit) s.habitNameLabel else s.goodHabitNameLabel,
                    placeholder = if (quit) s.habitNamePlaceholder else s.goodNamePlaceholder,
                )
                if (d is CoachAction.Build) BuildTargetCard(d, onChange = { draft = it }, s = s)
            }
            is CoachAction.Event -> {
                NixTextField(d.title, { draft = d.copy(title = it) }, label = s.eventNameLabel, placeholder = s.eventNamePlaceholder)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(s.eventCategoryLabel)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EventCategory.pickerOrder.forEach {
                            NixChip(s.categoryLabel(it), selected = it == d.category, onClick = { draft = d.copy(category = it) })
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(s.eventWhenLabel)
                    NixCard(onClick = { showPicker = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NixIconView(NixIcon.Calendar, c.accent, size = 22.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                formatDateTime(CoachActionRules.eventMillis(d, days), days, s),
                                style = NixType.BodyStrong,
                                color = c.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            NixIconView(NixIcon.Chevron, c.textMuted, size = 18.dp)
                        }
                    }
                }
                if (showPicker) {
                    DateTimePickerDialog(
                        initialEpochMillis = CoachActionRules.eventMillis(d, days),
                        onConfirm = { millis ->
                            draft = CoachActionRules.withEventTime(d, millis, days)
                            showPicker = false
                        },
                        onDismiss = { showPicker = false },
                    )
                }
            }
        }
        problem?.let { Text(problemText(it, draft, s), style = NixType.Small, color = c.accentWarm) }
    }

    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NixButton(s.cancel, onCancel, Modifier.weight(1f), style = NixButtonStyle.Secondary)
        NixButton(s.voiceEditKeep, { onKeep(draft) }, Modifier.weight(1f), icon = NixIcon.Tick, enabled = problem == null)
    }
}

/** Every day / days a week, and the target stepper: the same card as the good habit form. */
@Composable
private fun BuildTargetCard(build: CoachAction.Build, onChange: (CoachAction.Build) -> Unit, s: Strings) {
    val c = Nix.colors
    val weekly = build.daysPerWeek != null
    NixCard {
        Text(s.habitFrequency, style = NixType.BodyStrong, color = c.textPrimary)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NixChip(s.frequencyDaily, selected = !weekly, onClick = { onChange(build.copy(daysPerWeek = null)) })
            NixChip(s.frequencyWeekly, selected = weekly, onClick = { if (!weekly) onChange(build.copy(daysPerWeek = DEFAULT_DAYS_PER_WEEK)) })
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (weekly) s.weeklyTarget else s.dailyTarget, style = NixType.BodyStrong, color = c.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(if (weekly) s.weeklyTargetHint else s.dailyTargetHint, style = NixType.Small, color = c.textMuted)
            }
            if (build.daysPerWeek != null) {
                Stepper(build.daysPerWeek, { onChange(build.copy(daysPerWeek = it)) }, min = 1, max = WeeklyGoalCalculator.MAX_DAYS_PER_WEEK)
            } else {
                Stepper(build.perDay.coerceIn(1, MAX_DAILY_TARGET), { onChange(build.copy(perDay = it)) }, min = 1, max = MAX_DAILY_TARGET)
            }
        }
    }
}

internal fun problemText(problem: CoachActionProblem, action: CoachAction, s: Strings): String = when (problem) {
    CoachActionProblem.NameMissing -> s.voiceProblemName
    CoachActionProblem.NameTooLong -> s.voiceProblemNameTooLong(if (action is CoachAction.Event) MAX_EVENT_TITLE else CoachActionRules.MAX_HABIT_NAME)
    CoachActionProblem.DailyTarget -> s.voiceProblemDailyTarget
    CoachActionProblem.DaysAWeek -> s.voiceProblemDaysAWeek
    CoachActionProblem.BadDate -> s.voiceProblemBadDate
    CoachActionProblem.InThePast -> s.voiceProblemPast
}
