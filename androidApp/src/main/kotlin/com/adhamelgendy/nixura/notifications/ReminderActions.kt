package com.adhamelgendy.nixura.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.adhamelgendy.nixura.R
import com.adhamelgendy.nixura.domain.notification.NotificationAction
import com.adhamelgendy.nixura.domain.notification.NotificationActionType
import com.adhamelgendy.nixura.domain.notification.PlannedNotification
import com.adhamelgendy.nixura.domain.reminder.ReminderActionHandler

/**
 * Turns a planned reminder's buttons into Android notification actions. Each one targets the non-exported
 * [ReminderActionReceiver] (no activity starts, the app does not open) and carries a once-only token.
 */
internal object ReminderActions {
    const val ACTION = "com.adhamelgendy.nixura.REMINDER_ACTION"
    const val EXTRA_TOKEN = "token"
    const val EXTRA_TYPE = "type"
    const val EXTRA_HABIT = "habit"
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_SNOOZE = "snooze"
    const val EXTRA_POST_KEY = "post"
    const val EXTRA_THEME = "theme"
    const val EXTRA_TOPIC = "topic"

    fun build(context: Context, item: PlannedNotification): List<NotificationCompat.Action> =
        item.actions.map { action -> NotificationCompat.Action.Builder(iconOf(action), action.label, pendingIntent(context, item, action)).build() }

    private fun pendingIntent(context: Context, item: PlannedNotification, action: NotificationAction): PendingIntent {
        val token = ReminderActionHandler.tokenOf(item.key, item.triggerAtEpochMillis, action.type)
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(ACTION)
            // Unique per button: PendingIntents that differ only in extras would otherwise be merged.
            .setData(Uri.parse("nixura://reminder-action/${Uri.encode(token)}"))
            .putExtra(EXTRA_TOKEN, token)
            .putExtra(EXTRA_TYPE, action.type.name)
            .putExtra(EXTRA_HABIT, action.habitId)
            .putExtra(EXTRA_POST_KEY, item.postKey)
            .apply {
                action.prayerIndex?.let { putExtra(EXTRA_PRAYER, it) }
                action.snoozeMinutes?.let { putExtra(EXTRA_SNOOZE, it) }
                item.theme?.let { putExtra(EXTRA_THEME, it.name) }
                item.topic?.let { putExtra(EXTRA_TOPIC, it.name) }
            }
        return PendingIntent.getBroadcast(context, token.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun iconOf(action: NotificationAction): Int = when (action.type) {
        NotificationActionType.LOG_ONE -> R.drawable.ic_w_plus
        NotificationActionType.LOG_PRAYER -> R.drawable.ic_w_check
        NotificationActionType.SNOOZE -> R.drawable.ic_w_clock
    }
}
