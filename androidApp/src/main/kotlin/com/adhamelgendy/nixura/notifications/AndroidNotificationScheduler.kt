package com.adhamelgendy.nixura.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.adhamelgendy.nixura.domain.notification.NotificationArming
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.notification.NotificationScheduler
import com.adhamelgendy.nixura.domain.notification.PlannedNotification
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import org.json.JSONArray
import org.json.JSONObject

/**
 * Schedules planned notifications with [AlarmManager] and keeps the plan in SharedPreferences so
 * [BootReceiver] can restore it after a reboot or app update.
 *
 * Every notification has a user-visible time (event reminder, daily check-in at the chosen time, milestone), so all
 * of them are armed with [AlarmManager.setAlarmClock] when the user allows exact alarms (Android 12+ permission).
 * Measured on ColorOS 15 (Oppo Reno 10, 2026-11-21): `setExactAndAllowWhileIdle` was accepted as exact but the OS
 * still widened its delivery window to ~75% of the time left (a reminder 4 min ahead came ~3 min late); alarm clocks
 * keep a zero window and arrived within 50 ms. Side effect: Android treats them as alarms (status-bar alarm icon,
 * "next alarm" in the clock/lock screen; tapping it opens what the reminder is about). Without the permission they
 * fall back to inexact alarms, which Android may deliver up to an hour late.
 *
 * Every notification has a stable key (`event:<id>:<lead>`, `milestone:...`, `daily:<day>`). The key is both the
 * alarm's identity (data URI + request code) and the stored plan entry, so [replaceAll] cancels exactly the alarms
 * it armed before, including a reminder that was edited (new lead = new key) or removed (key missing).
 *
 * Which items are armed is decided by [NotificationArming]: a reminder whose time has passed but that the OS has not
 * delivered yet is armed again (it then fires at once) instead of being cancelled by the re-plan.
 * [ReminderReceiver] records each delivery with [markDelivered] so nothing is shown twice.
 */
class AndroidNotificationScheduler(context: Context) : NotificationScheduler {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun replaceAll(planned: List<PlannedNotification>) = synchronized(LOCK) {
        val stored = decode(prefs.getString(KEY_PLAN, null))
        stored.forEach { alarmManager.cancel(pendingIntent(it)) }
        val now = System.currentTimeMillis()
        val armed = NotificationArming.select(planned, stored, delivered(now), now)
        armed.forEach(::schedule)
        prefs.edit().putString(KEY_PLAN, encode(armed)).apply()
    }

    /** Re-arms the stored plan (after reboot or app update), including reminders that became due while the phone was off. */
    fun restoreFromStorage() = synchronized(LOCK) {
        val stored = decode(prefs.getString(KEY_PLAN, null))
        val now = System.currentTimeMillis()
        NotificationArming.select(stored, stored, delivered(now), now).forEach(::schedule)
    }

    /** Records that [key] (armed for [triggerAt]) was shown, so a later re-plan never arms it again. */
    fun markDelivered(key: String, triggerAt: Long) = synchronized(LOCK) {
        val now = System.currentTimeMillis()
        val log = delivered(now).toMutableMap()
        log[key] = triggerAt
        val json = JSONObject()
        log.forEach { (k, at) -> json.put(k, at) }
        prefs.edit().putString(KEY_DELIVERED, json.toString()).commit() // commit: the process may die right after
    }

    /** Delivery records still relevant to [NotificationArming] (older ones are dropped). */
    private fun delivered(now: Long): Map<String, Long> {
        val raw = prefs.getString(KEY_DELIVERED, null) ?: return emptyMap()
        val since = NotificationArming.keepDeliveredSince(now)
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.getLong(it) }.filterValues { it >= since }
        }.getOrDefault(emptyMap())
    }

    private fun schedule(item: PlannedNotification) {
        val operation = pendingIntent(item)
        try {
            if (AndroidNotificationPermission.canScheduleExactAlarms(appContext)) {
                val show = NotificationDeepLink.contentIntent(appContext, item.key, item.kind, item.targetId)
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(item.triggerAtEpochMillis, show), operation)
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
            .putExtra(ReminderReceiver.EXTRA_AT, item.triggerAtEpochMillis)
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
        const val KEY_DELIVERED = "delivered"

        /** One lock for every instance: the receivers create their own scheduler next to the app's Koin one. */
        val LOCK = Any()
    }
}
