package com.adhamelgendy.nixura.presentation.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.support.BugCategory
import com.adhamelgendy.nixura.domain.support.BugReportEmail
import com.adhamelgendy.nixura.domain.support.BugReportProblem
import com.adhamelgendy.nixura.domain.support.BugReportRules
import com.adhamelgendy.nixura.domain.support.CloudOutcome
import com.adhamelgendy.nixura.domain.support.KeepNixUraSummary
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixChip
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.components.NixTextField
import com.adhamelgendy.nixura.presentation.components.SubScreen
import com.adhamelgendy.nixura.presentation.components.Text
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.strings.FaqItem
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.strings.Strings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import org.koin.compose.viewmodel.koinViewModel

/**
 * Launcher shortcut "Don't delete NixUra!": funny, light guilt from the user's real numbers (owner decision 5).
 * Stay, feedback and uninstall are all one tap and the same size; the way out is never hidden.
 */
@Composable
fun KeepNixUraScreen(navigator: Navigator) {
    val s = LocalStrings.current
    val c = Nix.colors
    val vm: KeepNixUraViewModel = koinViewModel()
    val summary by vm.summary.collectAsStateWithLifecycle()
    var settingsFailed by rememberSaveable { mutableStateOf(false) }

    SubScreen(title = s.keepTitle, onBack = { navigator.back() }) {
        Text(s.keepHeadline, style = NixType.Title, color = c.textPrimary)
        Text(s.keepIntro, style = NixType.Body, color = c.textSecondary)

        // Nothing until the data is read (it is local, so this is a blink), then the real numbers.
        summary?.let { KeepProgressCard(it, s) }

        Text(s.keepCanChange, style = NixType.Body, color = c.textSecondary)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NixButton(s.keepBack, { navigator.popToTab() }, Modifier.fillMaxWidth(), icon = NixIcon.Home)
            NixButton(s.keepFeedback, { navigator.push(Route.ReportBug) }, Modifier.fillMaxWidth(), style = NixButtonStyle.Secondary, icon = NixIcon.Mail)
            // Same size as the others (owner rule: never hide or shrink the way out).
            NixButton(
                s.keepUninstall, { settingsFailed = !vm.openAppSettings() }, Modifier.fillMaxWidth(),
                style = NixButtonStyle.Secondary, icon = NixIcon.Trash,
            )
            Text(
                if (settingsFailed) s.keepSettingsFailed else s.keepUninstallNote,
                style = NixType.Small, color = if (settingsFailed) c.danger else c.textMuted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun KeepProgressCard(summary: KeepNixUraSummary, s: Strings) {
    val c = Nix.colors
    NixCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!summary.hasProgress) {
                Text(s.keepNoProgress, style = NixType.BodyStrong, color = c.textPrimary)
            } else {
                summary.bestStreakHabit?.let { habit ->
                    IconLine(NixIcon.Flame, s.keepStreak(habit, summary.bestStreakDays), c.accent)
                }
                summary.waterGlasses?.let { IconLine(NixIcon.Drop, s.keepWater(it), c.accent) }
                IconLine(NixIcon.Leaf, s.keepTracking(summary.quitHabits + summary.goodHabits), c.positive)
            }
            IconLine(
                if (summary.isGuest) NixIcon.Info else NixIcon.Shield,
                if (summary.isGuest) s.keepGuestNote else s.keepSignedInNote,
                c.textMuted,
                strong = false,
            )
        }
    }
}

@Composable
private fun IconLine(icon: NixIcon, text: String, tint: androidx.compose.ui.graphics.Color, strong: Boolean = true) {
    val c = Nix.colors
    Row(verticalAlignment = Alignment.Top) {
        NixIconView(icon, tint, size = 20.dp)
        Spacer(Modifier.width(12.dp))
        Text(text, style = if (strong) NixType.BodyStrong else NixType.Small, color = if (strong) c.textPrimary else c.textSecondary)
    }
}

/** Launcher shortcut "Bugs or issues?" (also reached from Help and the Keep screen). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportBugScreen(navigator: Navigator) {
    val s = LocalStrings.current
    val c = Nix.colors
    val vm: ReportBugViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val signedIn by vm.signedIn.collectAsStateWithLifecycle()

    SubScreen(title = s.bugTitle, onBack = { navigator.back() }) {
        val sent = state.result
        if (sent != null) {
            BugSentCard(sent.cloud, sent.emailOpened, s)
            NixButton(s.bugDone, { navigator.popToTab() }, Modifier.fillMaxWidth())
            return@SubScreen
        }

        Text(s.bugIntro, style = NixType.Body, color = c.textSecondary)

        Text(s.bugCategoryLabel, style = NixType.SmallStrong, color = c.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BugCategory.entries.forEach { category ->
                NixChip(s.bugCategory(category), selected = state.category == category, onClick = { vm.onCategory(category) })
            }
        }

        Column {
            NixTextField(
                state.message, vm::onMessage, label = s.bugMessageLabel, placeholder = s.bugMessagePlaceholder,
                singleLine = false, maxLength = BugReportRules.MAX_MESSAGE,
            )
            AnimatedVisibility(state.problem != null) {
                Text(
                    state.problem?.let { problemText(it, s) }.orEmpty(),
                    style = NixType.Small, color = c.danger, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        NixTextField(
            state.steps, vm::onSteps, label = s.bugStepsLabel, placeholder = s.bugStepsPlaceholder,
            singleLine = false, maxLength = BugReportRules.MAX_STEPS,
        )

        NixCard(Modifier.fillMaxWidth(), color = c.surfaceRaised, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                NixIconView(NixIcon.Lock, c.textMuted, size = 18.dp)
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.bugPrivacyNote, style = NixType.Small, color = c.textSecondary)
                    Text(if (signedIn) s.bugSignedInNote else s.bugGuestNote, style = NixType.Small, color = c.textMuted)
                }
            }
        }

        NixButton(s.bugSend, vm::onSend, Modifier.fillMaxWidth(), icon = NixIcon.Send, loading = state.sending)
    }
}

@Composable
private fun BugSentCard(cloud: CloudOutcome, emailOpened: Boolean, s: Strings) {
    val c = Nix.colors
    NixCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(s.bugSentTitle, style = NixType.Title, color = c.textPrimary)
            when (cloud) {
                CloudOutcome.SAVED -> Text(s.bugSentSaved, style = NixType.Body, color = c.positive)
                CloudOutcome.OFFLINE -> Text(s.bugSentOffline, style = NixType.Body, color = c.textSecondary)
                CloudOutcome.FAILED -> Text(s.bugSentFailed, style = NixType.Body, color = c.textSecondary)
                CloudOutcome.GUEST -> Unit
            }
            Text(
                if (emailOpened) s.bugSentEmailOpened else s.bugSentNoEmailApp(BugReportEmail.SUPPORT_ADDRESS),
                style = NixType.Body, color = c.textSecondary,
            )
        }
    }
}

private fun problemText(problem: BugReportProblem, s: Strings): String = when (problem) {
    BugReportProblem.EMPTY -> s.bugEmpty
    BugReportProblem.TOO_SHORT -> s.bugTooShort(BugReportRules.MIN_MESSAGE)
    BugReportProblem.TOO_LONG -> s.bugTooLong(BugReportRules.MAX_MESSAGE)
}

/** Launcher shortcut "Need help?": offline FAQ, one question open at a time, and a way to report a problem. */
@Composable
fun HelpScreen(navigator: Navigator) {
    val s = LocalStrings.current
    val c = Nix.colors
    var open by rememberSaveable { mutableStateOf(-1) }

    SubScreen(title = s.helpTitle, onBack = { navigator.back() }) {
        Text(s.helpIntro, style = NixType.Body, color = c.textSecondary)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            s.helpFaq.forEachIndexed { index, item ->
                FaqCard(item, expanded = open == index, onToggle = { open = if (open == index) -1 else index })
            }
        }
        NixCard(Modifier.fillMaxWidth()) {
            Text(s.helpStillStuck, style = NixType.Heading, color = c.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(s.helpStillStuckBody, style = NixType.Body, color = c.textSecondary)
            Spacer(Modifier.height(14.dp))
            NixButton(s.helpReport, { navigator.push(Route.ReportBug) }, Modifier.fillMaxWidth(), style = NixButtonStyle.Secondary, icon = NixIcon.Mail)
        }
    }
}

@Composable
private fun FaqCard(item: FaqItem, expanded: Boolean, onToggle: () -> Unit) {
    val c = Nix.colors
    NixCard(Modifier.fillMaxWidth(), onClick = onToggle, padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.question, style = NixType.BodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            // Chevron points to the end when closed and down when open. NixIconView mirrors it in RTL (it then points
            // left), so turning it down there is a quarter turn the other way.
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            NixIconView(
                NixIcon.Chevron, c.textMuted, size = 18.dp,
                modifier = Modifier.rotate(if (!expanded) 0f else if (rtl) -90f else 90f),
            )
        }
        AnimatedVisibility(expanded) {
            Text(item.answer, style = NixType.Body, color = c.textSecondary, modifier = Modifier.padding(top = 10.dp))
        }
    }
}
