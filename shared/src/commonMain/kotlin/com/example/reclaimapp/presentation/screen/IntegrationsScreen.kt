@file:OptIn(ExperimentalLayoutApi::class)

package com.example.reclaimapp.presentation.screen

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.integration.IntegrationError
import com.example.reclaimapp.domain.integration.IntegrationProvider
import com.example.reclaimapp.domain.integration.IntegrationState
import com.example.reclaimapp.domain.integration.NotionMapping
import com.example.reclaimapp.presentation.components.ConfirmDialog
import com.example.reclaimapp.presentation.components.NixButton
import com.example.reclaimapp.presentation.components.NixButtonStyle
import com.example.reclaimapp.presentation.components.NixCard
import com.example.reclaimapp.presentation.components.NixChip
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconView
import com.example.reclaimapp.presentation.components.SectionLabel
import com.example.reclaimapp.presentation.components.SubScreen
import com.example.reclaimapp.presentation.integrations.IntegrationNotice
import com.example.reclaimapp.presentation.integrations.IntegrationsViewModel
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.strings.Strings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Settings → Integrations: link Google Calendar and Notion, pick what to import, sync, disconnect. */
@Composable
fun IntegrationsScreen(navigator: Navigator) {
    val vm: IntegrationsViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val c = Nix.colors
    val uri = LocalUriHandler.current
    val clock = koinInject<Clock>()
    var confirmDisconnect by remember { mutableStateOf<IntegrationProvider?>(null) }
    // Coming back from Notion's page in the browser: the server now knows the link.
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }

    SubScreen(title = s.integrationsTitle, onBack = { navigator.back() }) {
        Text(s.integrationsIntro, style = NixType.Body, color = c.textSecondary)
        state.notice?.let { n ->
            NixCard(color = c.accentWarm.copy(alpha = 0.10f), borderColor = c.accentWarm.copy(alpha = 0.35f), onClick = vm::onNoticeShown) {
                Text(noticeText(n, s), style = NixType.Body, color = c.textPrimary)
            }
        }
        if (!state.signedIn) {
            NixCard {
                Text(s.integrationsNeedAccount, style = NixType.Body, color = c.textSecondary)
                Spacer(Modifier.height(12.dp))
                NixButton(s.authSignIn, { navigator.push(Route.Auth.Login) }, Modifier.fillMaxWidth())
            }
            return@SubScreen
        }

        // Google Calendar
        SectionLabel(s.googleCalendar)
        ProviderCard(
            name = s.googleCalendar,
            state = state.google,
            busy = state.busy == IntegrationProvider.GOOGLE,
            now = clock.nowEpochMillis(),
            onConnect = vm::onConnectGoogle,
            onSync = { vm.onSyncNow(IntegrationProvider.GOOGLE) },
            onDisconnect = { confirmDisconnect = IntegrationProvider.GOOGLE },
        ) {
            if (state.googleCalendars.isNotEmpty()) {
                Text(s.googlePickCalendars, style = NixType.SmallStrong, color = c.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.googleCalendars.forEach { cal ->
                        NixChip(cal.name.ifBlank { cal.id }, cal.id in state.google.selection, { vm.onToggleGoogleCalendar(cal.id) })
                    }
                }
            }
        }

        // Notion
        SectionLabel(s.notion)
        ProviderCard(
            name = s.notion,
            state = state.notion,
            busy = state.busy == IntegrationProvider.NOTION,
            now = clock.nowEpochMillis(),
            onConnect = { vm.onConnectNotion { url -> uri.openUri(url) } },
            onSync = { vm.onSyncNow(IntegrationProvider.NOTION) },
            onDisconnect = { confirmDisconnect = IntegrationProvider.NOTION },
        ) {
            Text(s.notionPickDatabase, style = NixType.SmallStrong, color = c.textSecondary)
            if (state.notionDatabases.isEmpty()) {
                Text(s.notionNoDatabases, style = NixType.Small, color = c.textMuted)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.notionDatabases.forEach { db ->
                    NixChip(db.title.ifBlank { db.id.take(8) }, db.id in state.notion.selection, { vm.onPickNotionDatabase(db) })
                }
            }
            val picked = state.notionDatabases.firstOrNull { it.id in state.notion.selection }
            if (picked != null) {
                if (picked.dateProperties.isEmpty()) {
                    Text(s.notionNoDateProperty, style = NixType.Small, color = c.danger)
                } else {
                    val title = state.notion.mapping[NotionMapping.TITLE]
                    val date = state.notion.mapping[NotionMapping.DATE]
                    if (picked.titleProperties.size > 1) {
                        Text(s.notionTitleProperty, style = NixType.SmallStrong, color = c.textSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            picked.titleProperties.forEach { p -> NixChip(p, p == title, { vm.onPickNotionDatabase(picked, titleProperty = p, dateProperty = date) }) }
                        }
                    }
                    Text(s.notionDateProperty, style = NixType.SmallStrong, color = c.textSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        picked.dateProperties.forEach { p -> NixChip(p, p == date, { vm.onPickNotionDatabase(picked, titleProperty = title, dateProperty = p) }) }
                    }
                }
            }
        }
    }

    confirmDisconnect?.let { provider ->
        val name = if (provider == IntegrationProvider.GOOGLE) s.googleCalendar else s.notion
        ConfirmDialog(
            title = s.disconnectTitle(name),
            message = s.disconnectBody,
            confirmText = s.integrationDisconnect,
            cancelText = s.cancel,
            danger = true,
            onConfirm = {
                confirmDisconnect = null
                vm.onDisconnect(provider)
            },
            onDismiss = { confirmDisconnect = null },
        )
    }
}

@Composable
private fun ProviderCard(
    name: String,
    state: IntegrationState,
    busy: Boolean,
    now: Long,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onDisconnect: () -> Unit,
    picker: @Composable () -> Unit,
) {
    val s = LocalStrings.current
    val c = Nix.colors
    NixCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NixIconView(NixIcon.Calendar, if (state.connected) c.positive else c.textMuted, size = 22.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = NixType.BodyStrong, color = c.textPrimary)
                Text(
                    if (state.connected) s.integrationConnectedAs(state.accountLabel) else s.integrationNotConnected,
                    style = NixType.Small,
                    color = if (state.connected) c.positive else c.textMuted,
                )
                if (state.connected) {
                    Text(
                        state.lastSyncedAt?.let { s.integrationSyncedAgo(agoText(now - it, s)) } ?: s.integrationNeverSynced,
                        style = NixType.Small,
                        color = c.textMuted,
                    )
                }
            }
        }
        state.lastError?.let { e ->
            Spacer(Modifier.height(10.dp))
            Text(errorText(e, s), style = NixType.Small, color = if (e == IntegrationError.REVOKED) c.danger else c.accentWarm)
        }
        Spacer(Modifier.height(14.dp))
        if (!state.connected) {
            NixButton(s.integrationConnect, onConnect, Modifier.fillMaxWidth(), loading = busy)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                picker()
                if (state.lastError == IntegrationError.REVOKED) {
                    NixButton(s.integrationReconnect, onConnect, Modifier.fillMaxWidth(), loading = busy)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NixButton(s.integrationSyncNow, onSync, Modifier.weight(1f), style = NixButtonStyle.Secondary, loading = busy)
                    NixButton(s.integrationDisconnect, onDisconnect, Modifier.weight(1f), style = NixButtonStyle.Danger)
                }
            }
        }
    }
}

private fun errorText(e: IntegrationError, s: Strings): String = when (e) {
    IntegrationError.REVOKED -> s.integrationErrRevoked
    IntegrationError.OFFLINE -> s.integrationErrOffline
    IntegrationError.NOT_CONFIGURED -> s.integrationErrNotConfigured
    IntegrationError.FAILED -> s.integrationErrFailed
}

private fun noticeText(n: IntegrationNotice, s: Strings): String = when (n) {
    IntegrationNotice.GOOGLE_NOT_AVAILABLE -> s.googleNotAvailable
    IntegrationNotice.GOOGLE_DENIED -> s.googleDenied
    IntegrationNotice.OFFLINE -> s.integrationErrOffline
    IntegrationNotice.FAILED -> s.integrationErrFailed
    IntegrationNotice.NOTION_NOT_CONNECTED -> s.notionNotConnected
}

private fun agoText(millis: Long, s: Strings): String {
    val minutes = millis.coerceAtLeast(0) / 60_000
    return when {
        minutes < 1 -> s.agoJustNow
        minutes < 60 -> s.agoMinutes(minutes)
        minutes < 24 * 60 -> s.agoHours(minutes / 60)
        else -> s.agoDays(minutes / (24 * 60))
    }
}
