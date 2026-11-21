package com.adhamelgendy.nixura.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.adhamelgendy.nixura.domain.notification.NotificationAccess
import com.adhamelgendy.nixura.domain.notification.NotificationPermissionGateway
import com.adhamelgendy.nixura.domain.notification.ReminderCapabilities
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import org.koin.compose.koinInject

/** Asks for notification permission in context. Get one with [rememberNotificationAsk]. */
fun interface NotificationAsk {
    /**
     * Call when the user just did something that needs notifications (added a reminder, turned reminders on).
     * Already allowed: nothing happens. Can ask: a one-line reason first, then the system dialog on "Allow".
     * Blocked for good: once per screen, a card that opens system settings. Never blocks the user's action itself.
     */
    fun ensure(reason: String)
}

/** The in-context permission flow (reason card → system dialog, or → system settings when Android won't ask again). */
@Composable
fun rememberNotificationAsk(): NotificationAsk {
    val s = LocalStrings.current
    val gateway = koinInject<NotificationPermissionGateway>()
    val capabilities = koinInject<ReminderCapabilities>()
    var reason by rememberSaveable { mutableStateOf<String?>(null) }
    var showBlocked by rememberSaveable { mutableStateOf(false) }
    var blockedShown by rememberSaveable { mutableStateOf(false) } // don't nag: the screen's notice card stays

    reason?.let { text ->
        ConfirmDialog(
            title = s.notificationAskTitle,
            message = text,
            confirmText = s.notificationAskAllow,
            cancelText = s.notNow,
            onConfirm = { reason = null; gateway.request() },
            onDismiss = { reason = null },
        )
    }
    if (showBlocked) {
        ConfirmDialog(
            title = s.notificationBlockedTitle,
            message = s.notificationBlockedBody,
            confirmText = s.reminderOpenSettings,
            cancelText = s.notNow,
            onConfirm = { showBlocked = false; capabilities.openNotificationSettings() },
            onDismiss = { showBlocked = false },
        )
    }
    return remember(gateway) {
        NotificationAsk { why ->
            when (gateway.access()) {
                NotificationAccess.ALLOWED -> Unit
                NotificationAccess.ASK -> reason = why
                NotificationAccess.OPEN_SETTINGS -> if (!blockedShown) {
                    blockedShown = true
                    showBlocked = true
                }
            }
        }
    }
}
