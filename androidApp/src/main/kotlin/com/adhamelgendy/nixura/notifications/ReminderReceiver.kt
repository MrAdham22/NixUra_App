package com.adhamelgendy.nixura.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.notification.NotificationSyncService
import com.adhamelgendy.nixura.domain.notification.NotificationTiming
import com.adhamelgendy.nixura.domain.notification.PlannedNotification
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.core.context.GlobalContext

/**
 * Fires when a scheduled alarm goes off and posts it through [NixNotifications] (event reminder, streak milestone,
 * daily check-in, good-habit reminder). Building the notification is not this class's job.
 *
 * Good-habit reminders (phase C) carry their buttons ([ReminderActions]), are posted under one key per habit (a new one
 * replaces the one showing), and re-plan afterwards so the next days stay armed even if the app is never opened.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_HAND_OFF) {
            // The soonest far reminder is now close: re-arm so it becomes an exact alarm clock (and set the next wake-up).
            AndroidNotificationScheduler(context).restoreFromStorage()
            return
        }
        val item = plannedItem(intent) ?: return
        val now = System.currentTimeMillis()
        val scheduler = AndroidNotificationScheduler(context)
        scheduler.markDelivered(item.key, item.triggerAtEpochMillis)
        Log.i(TAG, "deliver ${item.key} late=${now - item.triggerAtEpochMillis}ms")

        // A nudge that the OS held back for a long time (deep sleep) is no longer "the right moment": drop it. Never
        // shown later as "you missed it" (PLAN.md).
        val stale = item.timing == NotificationTiming.FLEXIBLE && now - item.triggerAtEpochMillis > FLEXIBLE_STALE_MILLIS
        if (!stale) {
            NixNotifications.post(
                context,
                NixNotificationRequest(
                    key = item.postKey,
                    kind = item.kind,
                    title = item.title,
                    body = item.body,
                    targetId = item.targetId,
                    theme = item.theme,
                    topic = item.topic,
                    actions = ReminderActions.build(context, item),
                    progress = item.progress,
                ),
            )
        }
        if (item.kind == NotificationKind.GOOD_HABIT) replan(context)
    }

    /** The planned item: the full JSON (phase C alarms) or the separate extras (alarms armed before it). */
    private fun plannedItem(intent: Intent): PlannedNotification? {
        intent.getStringExtra(EXTRA_PLAN)?.let { raw ->
            return runCatching { PlannedNotificationJson.decode(JSONObject(raw)) }.getOrNull()
        }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return null
        // Pre-phase-B alarms have no key extra, but their data URI is nixura://reminder/<key>.
        val key = intent.getStringExtra(EXTRA_KEY) ?: intent.data?.lastPathSegment ?: return null
        return PlannedNotification(
            key = key,
            triggerAtEpochMillis = intent.getLongExtra(EXTRA_AT, System.currentTimeMillis()),
            title = title,
            body = intent.getStringExtra(EXTRA_BODY).orEmpty(),
            kind = intent.getStringExtra(EXTRA_KIND)?.let { name -> NotificationKind.entries.firstOrNull { it.name == name } }
                ?: NotificationKind.fromKey(key),
            targetId = intent.getLongExtra(EXTRA_TARGET, -1L).takeIf { it > 0 },
            theme = intent.getStringExtra(EXTRA_THEME)?.let { name -> HabitTheme.entries.firstOrNull { it.name == name } },
        )
    }

    /** Extends the plan (today + tomorrow) from here, so reminders keep coming on days the app is not opened. */
    private fun replan(context: Context) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                GlobalContext.getOrNull()?.getOrNull<NotificationSyncService>()?.syncOnce()
            } catch (failure: Exception) {
                Log.w(TAG, "re-plan after delivery failed", failure)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.adhamelgendy.nixura.REMINDER"

        /** The hybrid-timing wake-up (see [AndroidNotificationScheduler]). Posts nothing. */
        const val ACTION_HAND_OFF = "com.adhamelgendy.nixura.REMINDER_HAND_OFF"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_KEY = "key"
        const val EXTRA_KIND = "kind"
        const val EXTRA_TARGET = "target"
        const val EXTRA_THEME = "theme"

        /** The whole planned item as JSON (phase C). */
        const val EXTRA_PLAN = "plan"

        /** The planned time, so the delivery can be recorded (see [AndroidNotificationScheduler.markDelivered]). */
        const val EXTRA_AT = "at"

        /** A flexible reminder delivered later than this is dropped. */
        private const val FLEXIBLE_STALE_MILLIS = 45 * 60_000L
        private const val TAG = "NixUraReminder"
    }
}
