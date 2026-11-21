package com.adhamelgendy.nixura.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.adhamelgendy.nixura.domain.notification.NotificationScheduler
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.notification.PlannedNotification
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import org.json.JSONArray
import org.json.JSONObject

/**
 * Schedules planned notifications with [AlarmManager] and keeps the plan in SharedPreferences so
 * [BootReceiver] can restore it after a reboot or app update.
 *
 * Event reminders use exact alarms when the user allows them (Android 12+ permission), otherwise they fall
 * back to inexact, doze-friendly alarms. Streak milestones and the daily check-in are always inexact.
 *
 * Every notification has a stable key (`event:<id>:<lead>`, `milestone:...`, `daily:<day>`). The key is both the
 * alarm's identity (data URI + request code) and the stored plan entry, so [replaceAll] cancels exactly the alarms
 * it armed before, including a reminder that was edited (new lead = new key) or removed (key missing).
 */
class AndroidNotificationScheduler(context: Context) : NotificationScheduler {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    override fun replaceAll(planned: List<PlannedNotification>) {
        cancelStored()
        val now = System.currentTimeMillis()
        val upcoming = planned.filter { it.triggerAtEpochMillis > now }.sortedBy { it.triggerAtEpochMillis }.take(MAX_SCHEDULED)
        upcoming.forEach(::schedule)
        prefs.edit().putString(KEY_PLAN, encode(upcoming)).apply()
    }

    /** Re-arms the stored plan (after reboot or app update). */
    @Synchronized
    fun restoreFromStorage() {
        val now = System.currentTimeMillis()
        decode(prefs.getString(KEY_PLAN, null)).filter { it.triggerAtEpochMillis > now }.forEach(::schedule)
    }

    private fun cancelStored() {
        decode(prefs.getString(KEY_PLAN, null)).forEach { alarmManager.cancel(pendingIntent(it)) }
    }

    private fun schedule(item: PlannedNotification) {
        val operation = pendingIntent(item)
        val exact = item.kind == NotificationKind.EVENT && AndroidNotificationPermission.canScheduleExactAlarms(appContext)
        try {
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.triggerAtEpochMillis, operation)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.triggerAtEpochMillis, operation)
            }
        } catch (failure: SecurityException) {
            // The exact-alarm permission was revoked between the check and the call: degrade to an inexact alarm.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.triggerAtEpochMillis, operation)
        }
    }

    private fun pendingIntent(item: PlannedNotification): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION)
            .setData(Uri.parse("nixura://reminder/${Uri.encode(item.key)}"))
            .putExtra(ReminderReceiver.EXTRA_TITLE, item.title)
            .putExtra(ReminderReceiver.EXTRA_BODY, item.body)
            .putExtra(ReminderReceiver.EXTRA_KEY, item.key)
            .putExtra(ReminderReceiver.EXTRA_KIND, item.kind.name)
            .apply {
                item.targetId?.let { putExtra(ReminderReceiver.EXTRA_TARGET, it) }
                item.theme?.let { putExtra(ReminderReceiver.EXTRA_THEME, it.name) }
            }
        return PendingIntent.getBroadcast(appContext, item.key.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun encode(items: List<PlannedNotification>): String {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("key", it.key)
                    .put("at", it.triggerAtEpochMillis)
                    .put("title", it.title)
                    .put("body", it.body)
                    .put("kind", it.kind.name)
                    .apply {
                        it.targetId?.let { id -> put("target", id) }
                        it.theme?.let { theme -> put("theme", theme.name) }
                    },
            )
        }
        return array.toString()
    }

    /** Reads the stored plan. Entries written before phase B have no kind / target / theme: the key prefix decides the kind. */
    private fun decode(raw: String?): List<PlannedNotification> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map {
                val o = array.getJSONObject(it)
                val key = o.getString("key")
                PlannedNotification(
                    key = key,
                    triggerAtEpochMillis = o.getLong("at"),
                    title = o.getString("title"),
                    body = o.getString("body"),
                    kind = o.optString("kind").let { name -> NotificationKind.entries.firstOrNull { k -> k.name == name } }
                        ?: NotificationKind.fromKey(key),
                    targetId = if (o.has("target")) o.getLong("target") else null,
                    theme = o.optString("theme").let { name -> HabitTheme.entries.firstOrNull { t -> t.name == name } },
                )
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS = "nixura_notifications"
        const val KEY_PLAN = "plan"
        const val MAX_SCHEDULED = 60
    }
}
