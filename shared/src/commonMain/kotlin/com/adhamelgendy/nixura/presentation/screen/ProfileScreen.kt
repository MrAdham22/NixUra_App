package com.adhamelgendy.nixura.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.components.ScoreKind
import com.adhamelgendy.nixura.presentation.components.ScoreSheet
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.presentation.components.Avatar
import com.adhamelgendy.nixura.presentation.components.ErrorBanner
import com.adhamelgendy.nixura.presentation.components.EventCard
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixProgressBar
import com.adhamelgendy.nixura.presentation.components.NixTextField
import com.adhamelgendy.nixura.presentation.components.SectionLabel
import com.adhamelgendy.nixura.presentation.components.SubScreen
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.profile.ProfileViewModel
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.util.formatDateTime
import com.adhamelgendy.nixura.presentation.util.rememberNowMillis
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
    var scoreSheet by remember { mutableStateOf<ScoreKind?>(null) }

    SubScreen(title = s.profileTitle, onBack = { navigator.back() }) {
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }

        NixCard {
            // Who this is, centered: picture, name, then the quieter details.
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(state.name.ifBlank { "N" }, size = 72.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    state.name.ifBlank { s.profileNoName },
                    style = NixType.Title, color = c.textPrimary, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (state.email.isNotBlank()) {
                    Text(state.email, style = NixType.Body, color = c.textSecondary, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                state.age?.let { Text(s.ageYears(it), style = NixType.Small, color = c.textMuted) }
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
                    if (state.accountEmail == null) {
                        NixTextField(email, { email = it }, label = s.emailLabel, placeholder = "name@example.com", keyboardType = KeyboardType.Email, maxLength = 80)
                    } else {
                        Text(s.accountEmailManaged, style = NixType.Small, color = c.textMuted)
                    }
                    NixTextField(age, { age = it.filter { ch -> ch.isDigit() } }, label = s.ageLabel, placeholder = "25", keyboardType = KeyboardType.Number, maxLength = 3)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NixButton(s.cancel, { editing = false }, Modifier.weight(1f), style = NixButtonStyle.Secondary)
                        NixButton(s.save, { vm.onSave(name, email, age) { editing = false } }, Modifier.weight(1f), loading = state.isSaving)
                    }
                }
            }
        }

        SectionLabel(s.yourProgress)
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScoreCard(s.quitScoreTitle, s.quitScoreBody, stats.quitScore, c.accent, s.noBadHabitsYet, Modifier.weight(1f).fillMaxHeight()) { scoreSheet = ScoreKind.BreakingFree }
            ScoreCard(s.disciplineScoreTitle, s.disciplineScoreBody, stats.disciplineScore, c.positive, if (stats.goodHabitCount > 0) s.disciplineWaiting else s.noGoodHabitsYet, Modifier.weight(1f).fillMaxHeight()) { scoreSheet = ScoreKind.Discipline }
        }

        SectionLabel(s.profileTotals)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(s.statClearDays, stats.totalClearDays.toString(), Modifier.weight(1f), info = s.infoClearDays)
            StatTile(s.statBestStreak, s.daysCount(stats.bestStreakDays.toInt()), Modifier.weight(1f), info = s.infoBestAcross)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(s.statCheckIns, stats.totalTimesDone.toString(), Modifier.weight(1f), info = s.infoCheckIns)
            StatTile(s.statHabitsTracked, (stats.badHabitCount + stats.goodHabitCount).toString(), Modifier.weight(1f), info = s.infoHabitsTracked)
        }

        SectionLabel(s.nearestEvent)
        val next = stats.nextEvent
        if (next != null) {
            EventCard(next, now, formatDateTime(next.targetAtEpochMillis, days, s), onClick = { navigator.push(Route.EventForm(next.id)) })
        } else {
            NixCard(Modifier.fillMaxWidth(), onClick = { navigator.push(Route.EventForm()) }) {
                Text(s.noUpcomingEvents, style = NixType.Body, color = c.textSecondary)
            }
        }
    }
    scoreSheet?.let { kind -> ScoreSheet(kind, stats, onDismiss = { scoreSheet = null }) }
}

/** One score as a tile: its name, the number, its bar, then one line of what it means. Tap for how it was worked out. */
@Composable
private fun ScoreCard(
    title: String,
    body: String,
    score: Int?,
    color: androidx.compose.ui.graphics.Color,
    emptyText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = Nix.colors
    NixCard(modifier, padding = PaddingValues(16.dp), onClick = if (score != null) onClick else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = NixType.BodyStrong, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (score != null) {
                Spacer(Modifier.width(6.dp))
                NixIconView(NixIcon.Chevron, c.textMuted, size = 16.dp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(score?.let { "$it%" } ?: "—", style = NixType.counter(30), color = if (score == null) c.textMuted else color)
        Spacer(Modifier.height(10.dp))
        NixProgressBar((score ?: 0) / 100f, color = color)
        Spacer(Modifier.height(10.dp))
        Text(if (score == null) emptyText else body, style = NixType.Small, color = c.textSecondary)
    }
}
