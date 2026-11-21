package com.adhamelgendy.nixura.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.adhamelgendy.nixura.domain.notification.NotificationArming
import com.adhamelgendy.nixura.domain.notification.NotificationTiming
import com.adhamelgendy.nixura.domain.notification.NotificationScheduler
import com.adhamelgendy.nixura.domain.notification.PlannedNotification
import org.json.JSONObject

/**
 * Schedules planned notifications with [AlarmManager] and keeps the plan in SharedPreferences so
 * [BootReceiver] can restore it after a reboot or app update.
 *
 * Timing is hybrid when the user allows exact alarms (Android 12+ permission). Measured on ColorOS 15 (Oppo Reno 10,
 * 2026-11-21): `setExactAndAllowWhileIdle` was accepted as exact but the OS still widened its delivery window to ~75%
 * of the time left, capped at 1 h (a reminder 4 min ahead came ~3 min late); only alarm clocks keep a zero window
 * (they arrived within 50 ms), but they show the status-bar alarm icon and "next alarm". So:
 * - a reminder at most [NotificationArming.ALARM_CLOCK_LEAD_MILLIS] away is an alarm clock (exact everywhere);
 * - a farther one is an ordinary exact alarm, plus one hand-off wake-up for the soonest of them, which re-arms the
 *   plan so that reminder becomes an alarm clock in time (the icon shows only 15-75 min before a reminder).
 * Without the permission everything is inexact (Android may deliver it up to an hour late).
 *
 * Phase C: [NotificationTiming.FLEXIBLE] items (water, a habit's chosen time) skip all of that: `setWindow` with a
 * 10-minute window, no exact-alarm permission and no alarm icon (see docs/ux-upgrade/C-reminders.md).
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
        val stored = PlannedNotificationJson.decodeAll(prefs.getString(KEY_PLAN, null))
        stored.forEach { alarmManager.cancel(pendingIntent(it)) }
        val now = System.currentTimeMillis()
        val armed = NotificationArming.select(planned, stored, delivered(now), now)
        arm(armed, now)
        prefs.edit().putString(KEY_PLAN, PlannedNotificationJson.encodeAll(armed)).apply()
    }

    /**
     * Re-arms the stored plan: after reboot or app update (including reminders that became due while the phone was
     * off), and at the hand-off wake-up, which turns the reminders now close by into alarm clocks.
     */
    fun restoreFromStorage() = synchronized(LOCK) {
        val stored = PlannedNotificationJson.decodeAll(prefs.getString(KEY_PLAN, null))
        val now = System.currentTimeMillis()
        arm(NotificationArming.select(stored, stored, delivered(now), now), now)
    }

    /** Arms every item and the one hand-off wake-up (see [NotificationArming.ALARM_CLOCK_LEAD_MILLIS]). */
    private fun arm(items: List<PlannedNotification>, now: Long) {
        val exact = AndroidNotificationPermission.canScheduleExactAlarms(appContext)
        items.forEach { schedule(it, now, exact) }
        val wake = handOffIntent()
        // Only on-time items need the hand-off; flexible ones are never alarm clocks.
        val handOffAt = if (exact) NotificationArming.handOffAt(items.filter { it.timing == NotificationTiming.ON_TIME }, now) else null
        if (handOffAt == null) {
            alarmManager.cancel(wake)
        } else {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, handOffAt, wake)
            } catch (failure: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, handOffAt, wake)
            }
        }
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

    /**
     * Arming the same [pendingIntent] again replaces its alarm, so the hand-off turns an ordinary exact alarm into an
     * alarm clock for the same reminder (never two alarms for one reminder).
     */
    private fun schedule(item: PlannedNotification, now: Long, exact: Boolean) {
        val operation = pendingIntent(item)
        val at = item.triggerAtEpochMillis
        try {
            when {
                // A nudge (water, a habit's chosen time): a short window, no exact-alarm permission, no alarm icon.
                // Android may hold it while the phone sleeps deeply (Doze) and deliver it when the screen wakes.
                item.timing == NotificationTiming.FLEXIBLE ->
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, at, FLEXIBLE_WINDOW_MILLIS, operation)
                !exact -> alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
                NotificationArming.armAsAlarmClock(at, now) -> {
                    val show = NotificationDeepLink.contentIntent(appContext, item.key, item.kind, item.targetId)
                    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), operation)
                }
                // Far ahead: on time on most phones; on ColorOS possibly late, but the hand-off re-arms it in time.
                else -> alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            }
        } catch (failure: SecurityException) {
            // The exact-alarm permission was revoked between the check and the call: degrade to an inexact alarm.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.triggerAtEpochMillis, operation)
        }
    }

    private fun handOffIntent(): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_HAND_OFF)
        return PendingIntent.getBroadcast(appContext, HAND_OFF_REQUEST, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
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
            // The whole item (post key, buttons, progress, timing), so the receiver shows exactly what was planned.
            .putExtra(ReminderReceiver.EXTRA_PLAN, PlannedNotificationJson.encode(item).toString())
            .apply {
                item.targetId?.let { putExtra(ReminderReceiver.EXTRA_TARGET, it) }
                item.theme?.let { putExtra(ReminderReceiver.EXTRA_THEME, it.name) }
            }
        return PendingIntent.getBroadcast(appContext, item.key.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private companion object {
        const val PREFS = "nixura_notifications"
        const val KEY_PLAN = "plan"
        const val KEY_DELIVERED = "delivered"
        const val HAND_OFF_REQUEST = 0x4E58 // fixed: one hand-off wake-up for the whole plan

        /** Delivery window of a flexible reminder (Android 12+ allows no less than 10 minutes). */
        const val FLEXIBLE_WINDOW_MILLIS = 10 * 60_000L

        /** One lock for every instance: the receivers create their own scheduler next to the app's Koin one. */
        val LOCK = Any()
    }
}
