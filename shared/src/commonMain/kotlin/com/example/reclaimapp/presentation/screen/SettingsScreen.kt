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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.domain.notification.NotificationPermissionGateway
import com.example.reclaimapp.presentation.components.ConfirmDialog
import com.example.reclaimapp.presentation.components.ErrorBanner
import com.example.reclaimapp.presentation.components.NixButton
import com.example.reclaimapp.presentation.components.NixButtonStyle
import com.example.reclaimapp.presentation.components.NixCard
import com.example.reclaimapp.presentation.components.NixChip
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconView
import com.example.reclaimapp.presentation.components.NixSwitch
import com.example.reclaimapp.presentation.components.SectionLabel
import com.example.reclaimapp.presentation.components.SubScreen
import com.example.reclaimapp.presentation.components.TimePickerDialog
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.settings.SettingsViewModel
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.strings.availableLanguages
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import com.example.reclaimapp.presentation.util.formatClockMinutes
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(navigator: Navigator) {
    val vm: SettingsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val permission = koinInject<NotificationPermissionGateway>()
    val s = LocalStrings.current
    val c = Nix.colors
    val settings = state.settings
    var showTime by remember { mutableStateOf(false) }
    var confirmErase by remember { mutableStateOf(false) }

    SubScreen(title = s.settingsTitle, onBack = { navigator.back() }) {
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }

        SectionLabel(s.appearance)
        NixCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeOption(s.darkMode, NixIcon.Moon, settings.darkTheme, Modifier.weight(1f)) { vm.onDarkTheme(true) }
                ThemeOption(s.lightMode, NixIcon.Sun, !settings.darkTheme, Modifier.weight(1f)) { vm.onDarkTheme(false) }
            }
        }

        SectionLabel(s.language)
        NixCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NixIconView(NixIcon.Globe, c.accent, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableLanguages.forEach { lang ->
                        NixChip(lang.nativeName, selected = settings.languageCode == lang.code, onClick = { vm.onLanguage(lang.code) })
                    }
                }
            }
        }

        SectionLabel(s.notifications)
        NixCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.notificationsToggle, style = NixType.BodyStrong, color = c.textPrimary)
                    Text(s.notificationsHint, style = NixType.Small, color = c.textMuted)
                }
                NixSwitch(settings.notificationsEnabled, { enabled ->
                    if (enabled && !permission.isGranted()) permission.request()
                    vm.onNotifications(enabled)
                })
            }
            if (settings.notificationsEnabled) {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NixIconView(NixIcon.Clock, c.accentWarm, size = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.dailyCheckIn, style = NixType.BodyStrong, color = c.textPrimary)
                        Text(s.dailyCheckInHint, style = NixType.Small, color = c.textMuted)
                    }
                    NixButton(formatClockMinutes(settings.dailyCheckInMinutes, s), { showTime = true }, style = NixButtonStyle.Secondary)
                }
            }
        }

        SectionLabel(s.dataTitle)
        NixCard {
            Text(s.dataBody, style = NixType.Small, color = c.textSecondary)
            Spacer(Modifier.height(12.dp))
            NixButton(s.eraseData, { confirmErase = true }, style = NixButtonStyle.Danger, icon = NixIcon.Trash, modifier = Modifier.fillMaxWidth())
        }

        SectionLabel(s.about)
        NixCard {
            Text("NixUra", style = NixType.Heading, color = c.textPrimary)
            Text(s.versionLabel, style = NixType.Small, color = c.textMuted)
            Spacer(Modifier.height(8.dp))
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
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NixIconView(icon, if (selected) c.accent else c.textSecondary, size = 26.dp)
            Text(label, style = NixType.SmallStrong, color = if (selected) c.textPrimary else c.textSecondary)
        }
    }
}
