package com.example.reclaimapp.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.presentation.components.Avatar
import com.example.reclaimapp.presentation.components.ErrorBanner
import com.example.reclaimapp.presentation.components.EventCard
import com.example.reclaimapp.presentation.components.NixButton
import com.example.reclaimapp.presentation.components.NixButtonStyle
import com.example.reclaimapp.presentation.components.NixCard
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconButton
import com.example.reclaimapp.presentation.components.NixProgressBar
import com.example.reclaimapp.presentation.components.NixTextField
import com.example.reclaimapp.presentation.components.SectionLabel
import com.example.reclaimapp.presentation.components.SubScreen
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.profile.ProfileViewModel
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import com.example.reclaimapp.presentation.util.formatDateTime
import com.example.reclaimapp.presentation.util.rememberNowMillis
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(navigator: Navigator) {
    val vm: ProfileViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val now by rememberNowMillis()
    val days = koinInject<DayCalculator>()
    val s = LocalStrings.current
    val c = Nix.colors
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    val stats = state.stats

    SubScreen(
        title = s.profileTitle,
        onBack = { navigator.back() },
        actions = { NixIconButton(NixIcon.Gear, { navigator.push(Route.Settings) }) },
    ) {
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }

        NixCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(state.name.ifBlank { "N" }, size = 64.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.name.ifBlank { s.profileNoName }, style = NixType.Title, color = c.textPrimary)
                    if (state.email.isNotBlank()) Text(state.email, style = NixType.Body, color = c.textSecondary)
                    state.age?.let { Text(s.ageYears(it), style = NixType.Small, color = c.textMuted) }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (!editing) {
                NixButton(
                    s.editProfile,
                    { name = state.name; email = state.email; age = state.age?.toString().orEmpty(); editing = true },
                    style = NixButtonStyle.Secondary,
                    icon = NixIcon.Pencil,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NixTextField(name, { name = it }, label = s.nameLabel, placeholder = s.namePlaceholder, maxLength = 40)
                    NixTextField(email, { email = it }, label = s.emailLabel, placeholder = "name@example.com", keyboardType = KeyboardType.Email, maxLength = 80)
                    NixTextField(age, { age = it.filter { ch -> ch.isDigit() } }, label = s.ageLabel, placeholder = "25", keyboardType = KeyboardType.Number, maxLength = 3)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NixButton(s.cancel, { editing = false }, Modifier.weight(1f), style = NixButtonStyle.Secondary)
                        NixButton(s.save, { vm.onSave(name, email, age) { editing = false } }, Modifier.weight(1f), loading = state.isSaving)
                    }
                }
            }
        }

        SectionLabel(s.yourProgress)
        ScoreCard(s.quitScoreTitle, s.quitScoreBody, stats.quitScore, c.accent, s.noBadHabitsYet)
        ScoreCard(s.disciplineScoreTitle, s.disciplineScoreBody, stats.disciplineScore, c.positive, s.noGoodHabitsYet)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(s.statClearDays, stats.totalClearDays.toString(), Modifier.weight(1f))
            StatTile(s.statBestStreak, s.daysCount(stats.bestStreakDays.toInt()), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(s.statCheckIns, stats.totalTimesDone.toString(), Modifier.weight(1f))
            StatTile(s.statHabitsTracked, (stats.badHabitCount + stats.goodHabitCount).toString(), Modifier.weight(1f))
        }

        SectionLabel(s.nearestEvent)
        val next = stats.nextEvent
        if (next != null) {
            EventCard(next, now, formatDateTime(next.targetAtEpochMillis, days, s), onClick = { navigator.push(Route.EventForm(next.id)) })
        } else {
            NixCard(onClick = { navigator.push(Route.EventForm()) }) {
                Text(s.noUpcomingEvents, style = NixType.Body, color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun ScoreCard(title: String, body: String, score: Int?, color: androidx.compose.ui.graphics.Color, emptyText: String) {
    val c = Nix.colors
    NixCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = NixType.Heading, color = c.textPrimary)
                Text(if (score == null) emptyText else body, style = NixType.Small, color = c.textSecondary)
            }
            Text(score?.let { "$it%" } ?: "—", style = NixType.counter(30), color = if (score == null) c.textMuted else color)
        }
        Spacer(Modifier.height(12.dp))
        NixProgressBar((score ?: 0) / 100f, color = color)
    }
}
