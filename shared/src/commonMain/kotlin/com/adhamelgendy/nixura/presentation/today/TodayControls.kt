package com.adhamelgendy.nixura.presentation.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.notification.ReminderCapabilities
import com.adhamelgendy.nixura.presentation.components.ConfirmDialog
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.components.NixSwitch
import com.adhamelgendy.nixura.presentation.components.Text
import com.adhamelgendy.nixura.presentation.components.rememberNotificationAsk
import com.adhamelgendy.nixura.presentation.deletion.SheetAction
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings, Notifications: the Today switch and, once it is on, its quit-streak opt-in. [resumeTick] re-reads what
 * the system allows. Draws nothing where Today is not supported.
 */
@Composable
fun TodaySettingsRows(resumeTick: Int) {
    val vm: TodayViewModel = koinViewModel()
    if (!vm.isSupported) return
    val state by vm.uiState.collectAsStateWithLifecycle()
    val capabilities = koinInject<ReminderCapabilities>()
    val ask = rememberNotificationAsk()
    val s = LocalStrings.current
    val c = Nix.colors
    val channelOn = remember(resumeTick, state.enabled) { !capabilities.notificationsAllowed() || capabilities.channelEnabled(NotificationKind.TODAY) }

    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NixIconView(NixIcon.Lock, c.accent, size = 22.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.todaySettingTitle, style = NixType.BodyStrong, color = c.textPrimary)
            val warns = state.enabled && (!channelOn || state.hiddenToday)
            Text(
                when {
                    !warns -> s.todaySettingHint
                    !channelOn -> s.todayChannelOff
                    else -> s.todayHiddenUntilTomorrow
                },
                style = NixType.Small,
                color = if (warns) c.accentWarm else c.textMuted,
            )
        }
        Spacer(Modifier.width(8.dp))
        NixSwitch(state.enabled, { on ->
            if (on) ask.ensure(s.notificationAskTodayReason)
            vm.setEnabled(on)
        })
    }
    if (!state.enabled) return
    if (!channelOn) {
        Spacer(Modifier.height(10.dp))
        NixButton(
            s.turnOnInSettings,
            { capabilities.openChannelSettings(NotificationKind.TODAY) },
            style = NixButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NixIconView(NixIcon.Flame, c.accent, size = 22.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.todayStreakTitle, style = NixType.BodyStrong, color = c.textPrimary)
            Text(s.todayStreakHint, style = NixType.Small, color = c.textMuted)
        }
        Spacer(Modifier.width(8.dp))
        NixSwitch(state.showStreak, vm::setShowStreak)
    }
}

/** The row in a good habit's long-press sheet; `null` where Today cannot show. */
@Composable
fun rememberTodaySheetAction(): SheetAction? {
    val vm: TodayViewModel = koinViewModel()
    if (!vm.isSupported) return null
    val state by vm.uiState.collectAsStateWithLifecycle()
    val ask = rememberNotificationAsk()
    val s = LocalStrings.current
    if (!state.notificationsOn) return null
    return SheetAction(s.todaySheetRow(state.enabled), NixIcon.Lock) {
        val on = !state.enabled
        if (on) ask.ensure(s.notificationAskTodayReason)
        vm.setEnabled(on)
    }
}

/** Offers Today once, when the user adds a second good habit. Never at first launch. */
@Composable
fun TodayOffer() {
    val vm: TodayViewModel = koinViewModel()
    if (!vm.isSupported) return
    val state by vm.uiState.collectAsStateWithLifecycle()
    val ask = rememberNotificationAsk()
    val s = LocalStrings.current
    var lastCount by rememberSaveable { mutableStateOf<Int?>(null) }
    var offering by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.goodHabitCount) {
        val count = state.goodHabitCount ?: return@LaunchedEffect
        if (lastCount == 1 && count == 2 && state.notificationsOn && !state.enabled && !state.offerSeen) offering = true
        lastCount = count
    }
    if (!offering) return
    ConfirmDialog(
        title = s.todayOfferTitle,
        message = s.todayOfferBody,
        confirmText = s.todayOfferConfirm,
        cancelText = s.notNow,
        onConfirm = {
            offering = false
            vm.setEnabled(true)
            ask.ensure(s.notificationAskTodayReason)
        },
        onDismiss = {
            offering = false
            vm.declineOffer()
        },
    )
}
