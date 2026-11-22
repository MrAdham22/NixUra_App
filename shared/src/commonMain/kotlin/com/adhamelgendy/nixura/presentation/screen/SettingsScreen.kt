package com.adhamelgendy.nixura.presentation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.adhamelgendy.nixura.presentation.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.adhamelgendy.nixura.presentation.locale.LocalAppLocale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.auth.SessionManager
import com.adhamelgendy.nixura.domain.model.DigitStyle
import com.adhamelgendy.nixura.domain.auth.SessionState
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.adhamelgendy.nixura.domain.notification.ReminderCapabilities
import com.adhamelgendy.nixura.presentation.components.rememberNotificationAsk
import com.adhamelgendy.nixura.presentation.components.ConfirmDialog
import com.adhamelgendy.nixura.presentation.components.ErrorBanner
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixChip
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.components.NixSwitch
import com.adhamelgendy.nixura.presentation.components.SectionLabel
import com.adhamelgendy.nixura.presentation.components.SubScreen
import com.adhamelgendy.nixura.presentation.components.TimePickerDialog
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.settings.SettingsViewModel
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.strings.availableLanguages
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.util.formatClockMinutes
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(navigator: Navigator) {
    val vm: SettingsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val notificationAsk = rememberNotificationAsk()
    val capabilities = koinInject<ReminderCapabilities>()
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ } // re-read after the system dialog or settings
    val notificationsAllowed = remember(resumeTick) { capabilities.notificationsAllowed() }
    val session by koinInject<SessionManager>().state.collectAsStateWithLifecycle()
    val signedInEmail = (session as? SessionState.SignedIn)?.user?.email
    val s = LocalStrings.current
    val c = Nix.colors
    val settings = state.settings
    var showTime by remember { mutableStateOf(false) }
    var confirmErase by remember { mutableStateOf(false) }

    SubScreen(title = s.settingsTitle, onBack = { navigator.back() }) {
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }

        // Every row reads the same way: an accent icon, a title with a muted line under it, then what you can do.
        SectionLabel(s.accountTitle)
        NixCard(onClick = { navigator.push(Route.Account) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NixIconView(NixIcon.User, c.accent, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(signedInEmail ?: s.accountGuestName, style = NixType.BodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.settingsAccountRow, style = NixType.Small, color = c.textMuted)
                }
                Spacer(Modifier.width(8.dp))
                NixIconView(NixIcon.Chevron, c.textMuted, size = 16.dp)
            }
        }
        NixCard(onClick = { navigator.push(Route.Integrations) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NixIconView(NixIcon.Calendar, c.accent, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.integrationsRow, style = NixType.BodyStrong, color = c.textPrimary)
                    Text(s.integrationsRowHint, style = NixType.Small, color = c.textMuted)
                }
                Spacer(Modifier.width(8.dp))
                NixIconView(NixIcon.Chevron, c.textMuted, size = 16.dp)
            }
        }

        SectionLabel(s.appearance)
        NixCard {
            // Same height for all three, even when "Same as phone" wraps.
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeOption(s.darkMode, NixIcon.Moon, !settings.followSystemTheme && settings.darkTheme, Modifier.weight(1f).fillMaxHeight()) { vm.onDarkTheme(true) }
                ThemeOption(s.lightMode, NixIcon.Sun, !settings.followSystemTheme && !settings.darkTheme, Modifier.weight(1f).fillMaxHeight()) { vm.onDarkTheme(false) }
                ThemeOption(s.systemMode, NixIcon.Phone, settings.followSystemTheme, Modifier.weight(1f).fillMaxHeight()) { vm.onFollowSystemTheme() }
            }
        }
        NixCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NixIconView(NixIcon.Globe, c.accent, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.language, style = NixType.BodyStrong, color = c.textPrimary)
                    Text(s.languageHint, style = NixType.Small, color = c.textMuted)
                }
            }
            Spacer(Modifier.height(12.dp))
            // Each language is written in its own language; "System default" in the language now shown.
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                availableLanguages.forEach { lang ->
                    NixChip(
                        lang.nativeName ?: s.languageSystem,
                        selected = settings.languageCode == lang.code,
                        onClick = { vm.onLanguage(lang.code) },
                    )
                }
            }
        }
        NixCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NixIconView(NixIcon.Hash, c.accent, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.digitsTitle, style = NixType.BodyStrong, color = c.textPrimary)
                    Text(s.digitsHint, style = NixType.Small, color = c.textMuted)
                }
                Spacer(Modifier.width(8.dp))
                // Each option shows its own digits, whatever the current setting (so "123" is never drawn as "١٢٣").
                CompositionLocalProvider(LocalAppLocale provides LocalAppLocale.current.copy(digitStyle = DigitStyle.WESTERN)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NixChip(s.digitsWestern, selected = settings.digitStyle == DigitStyle.WESTERN, onClick = { vm.onDigits(DigitStyle.WESTERN) })
                        NixChip(s.digitsArabic, selected = settings.digitStyle == DigitStyle.ARABIC_INDIC, onClick = { vm.onDigits(DigitStyle.ARABIC_INDIC) })
                    }
                }
            }
        }

        SectionLabel(s.notifications)
        NixCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NixIconView(NixIcon.Bell, c.accent, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.notificationsToggle, style = NixType.BodyStrong, color = c.textPrimary)
                    Text(s.notificationsHint, style = NixType.Small, color = c.textMuted)
                }
                Spacer(Modifier.width(8.dp))
                NixSwitch(settings.notificationsEnabled, { enabled ->
                    if (enabled) notificationAsk.ensure(s.notificationAskSettingsReason)
                    vm.onNotifications(enabled)
                })
            }
            if (settings.notificationsEnabled) {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NixIconView(NixIcon.Clock, c.accent, size = 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.dailyCheckIn, style = NixType.BodyStrong, color = c.textPrimary)
                        Text(s.dailyCheckInHint, style = NixType.Small, color = c.textMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    NixButton(formatClockMinutes(settings.dailyCheckInMinutes, s), { showTime = true }, style = NixButtonStyle.Secondary)
                }
                // Per-type sound / silence lives in Android's channel screen; the app only links to it (no duplicate
                // toggles). When the system blocks everything, say so here, where the user expects reminders.
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    NixIconView(if (notificationsAllowed) NixIcon.Gear else NixIcon.Info, if (notificationsAllowed) c.accent else c.accentWarm, size = 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.notificationTypes, style = NixType.BodyStrong, color = c.textPrimary)
                        Text(
                            if (notificationsAllowed) s.notificationTypesHint else s.notificationsBlockedNotice,
                            style = NixType.Small,
                            color = if (notificationsAllowed) c.textMuted else c.accentWarm,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    NixButton(s.reminderOpenSettings, capabilities::openNotificationSettings, style = NixButtonStyle.Secondary)
                }
                // Phase D: the opt-in Today notification on the lock screen.
                com.adhamelgendy.nixura.presentation.today.TodaySettingsRows(resumeTick)
                // Phase C: pause every good-habit reminder on this phone (each habit's own switch is in its sheet).
                Spacer(Modifier.height(14.dp))
                PauseHabitRemindersRow()
            }
        }

        SectionLabel(s.privacyAndData)
        NixCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NixIconView(NixIcon.Sparkle, c.accent, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.aiSettingTitle, style = NixType.BodyStrong, color = c.textPrimary)
                    Text(s.aiSettingHint, style = NixType.Small, color = c.textMuted)
                }
                Spacer(Modifier.width(8.dp))
                NixSwitch(settings.aiCoachConsent == true, vm::onAiCoach)
            }
        }
        NixCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NixIconView(NixIcon.Shield, c.accent, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.usageStatsToggle, style = NixType.BodyStrong, color = c.textPrimary)
                    Text(s.usageStatsHint, style = NixType.Small, color = c.textMuted)
                }
                Spacer(Modifier.width(8.dp))
                NixSwitch(settings.usageStatsEnabled, vm::onUsageStats)
            }
        }
        NixCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NixIconView(NixIcon.Archive, c.accent, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.dataTitle, style = NixType.BodyStrong, color = c.textPrimary)
                    Text(s.dataBody, style = NixType.Small, color = c.textMuted)
                }
            }
            Spacer(Modifier.height(14.dp))
            NixButton(s.eraseData, { confirmErase = true }, style = NixButtonStyle.Danger, icon = NixIcon.Trash, modifier = Modifier.fillMaxWidth())
        }

        SectionLabel(s.about)
        NixCard(
            onClick = {
                navigator.selectTab(Route.Home)
                vm.onReplayGuide()
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NixIconView(NixIcon.Sparkle, c.accent, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.guideReplay, style = NixType.BodyStrong, color = c.textPrimary)
                    Text(s.guideReplayHint, style = NixType.Small, color = c.textMuted)
                }
                Spacer(Modifier.width(8.dp))
                NixIconView(NixIcon.Chevron, c.textMuted, size = 16.dp)
            }
        }
        NixCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NixUra", style = NixType.BodyStrong, color = c.textPrimary)
                    Text(s.versionLabel, style = NixType.Small, color = c.textMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(s.aboutBody, style = NixType.Small, color = c.textSecondary)
        }
    }

    if (showTime) {
        TimePickerDialog(
            initialMinutes = settings.dailyCheckInMinutes,
            title = s.dailyCheckIn,
            onConfirm = { vm.onCheckInMinutes(it); showTime = false },
            onDismiss = { showTime = false },
        )
    }
    if (confirmErase) {
        ConfirmDialog(
            title = s.eraseTitle,
            message = s.eraseBody,
            confirmText = s.eraseConfirm,
            cancelText = s.cancel,
            danger = true,
            onConfirm = {
                confirmErase = false
                navigator.selectTab(Route.Home)
                vm.onEraseAllData()
            },
            onDismiss = { confirmErase = false },
        )
    }
}

@Composable
private fun ThemeOption(label: String, icon: NixIcon, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Nix.colors
    NixCard(
        modifier = modifier,
        onClick = onClick,
        color = if (selected) c.accent.copy(alpha = 0.16f) else c.surfaceRaised,
        borderColor = if (selected) c.accent else c.border,
        padding = androidx.compose.foundation.layout.PaddingValues(14.dp),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            NixIconView(icon, if (selected) c.accent else c.textSecondary, size = 26.dp)
            Text(label, style = NixType.SmallStrong, color = if (selected) c.textPrimary else c.textSecondary)
        }
    }
}

/** Settings → Notifications: "Pause habit reminders" for today or a week, on this phone only (PLAN.md decision 2). */
@Composable
private fun PauseHabitRemindersRow() {
    val s = LocalStrings.current
    val c = Nix.colors
    val vm: com.adhamelgendy.nixura.presentation.goodhabits.reminders.HabitRemindersViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val clock = koinInject<com.adhamelgendy.nixura.domain.clock.Clock>()
    val days = koinInject<com.adhamelgendy.nixura.domain.time.DayCalculator>()
    val pausedUntil = state.device.pausedUntilEpochMillis?.takeIf { it > clock.nowEpochMillis() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NixIconView(NixIcon.Bell, if (pausedUntil != null) c.accentWarm else c.accent, size = 22.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.pauseRemindersTitle, style = NixType.BodyStrong, color = c.textPrimary)
            Text(
                pausedUntil?.let { s.reminderStatusPausedUntil(com.adhamelgendy.nixura.presentation.util.formatDate(days.dayIndexOf(it), s, includeWeekday = true, includeYear = false)) }
                    ?: s.pauseRemindersHint,
                style = NixType.Small,
                color = if (pausedUntil != null) c.accentWarm else c.textMuted,
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (pausedUntil != null) {
            NixButton(s.reminderResume, vm::resumeAll, style = NixButtonStyle.Secondary)
        } else {
            NixChip(s.pauseForToday, selected = false, onClick = { vm.pauseAllFor(1) })
            NixChip(s.pauseForWeek, selected = false, onClick = { vm.pauseAllFor(7) })
        }
    }
}
