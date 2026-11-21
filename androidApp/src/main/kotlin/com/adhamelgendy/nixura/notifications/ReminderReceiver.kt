package com.adhamelgendy.nixura.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.theme.HabitTheme

/**
 * Fires when a scheduled alarm goes off and posts it through [NixNotifications] (event reminder, streak milestone,
 * daily check-in; good-habit reminders from phase C). Building the notification is not this class's job.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        // Pre-phase-B alarms have no key extra, but their data URI is nixura://reminder/<key>.
        val key = intent.getStringExtra(EXTRA_KEY) ?: intent.data?.lastPathSegment ?: return
        // Alarms armed before phase B carry no kind: the key prefix says what they are.
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { name -> NotificationKind.entries.firstOrNull { it.name == name } }
            ?: NotificationKind.fromKey(key)
        val triggerAt = intent.getLongExtra(EXTRA_AT, 0L)
        if (triggerAt > 0L) {
            AndroidNotificationScheduler(context).markDelivered(key, triggerAt)
            Log.i(TAG, "deliver $key late=${System.currentTimeMillis() - triggerAt}ms")
        }
        NixNotifications.post(
            context,
            NixNotificationRequest(
                key = key,
                kind = kind,
                title = title,
                body = intent.getStringExtra(EXTRA_BODY).orEmpty(),
                targetId = intent.getLongExtra(EXTRA_TARGET, -1L).takeIf { it > 0 },
                theme = intent.getStringExtra(EXTRA_THEME)?.let { name -> HabitTheme.entries.firstOrNull { it.name == name } },
            ),
        )
    }

    companion object {
        const val ACTION = "com.adhamelgendy.nixura.REMINDER"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_KEY = "key"
        const val EXTRA_KIND = "kind"
        const val EXTRA_TARGET = "target"
        const val EXTRA_THEME = "theme"

        /** The planned time, so the delivery can be recorded (see [AndroidNotificationScheduler.markDelivered]). */
        const val EXTRA_AT = "at"
        private const val TAG = "NixUraReminder"
    }
}
