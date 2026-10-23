package com.example.reclaimapp.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.reclaimapp.domain.notification.NotificationScheduler
import com.example.reclaimapp.domain.notification.PlannedNotification
import org.json.JSONArray
import org.json.JSONObject

/**
 * Schedules planned notifications with [AlarmManager] (inexact, doze-friendly: no special permission needed)
 * and keeps the plan in SharedPreferences so [BootReceiver] can restore it after a reboot.
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
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.triggerAtEpochMillis, pendingIntent(item))
    }

    private fun pendingIntent(item: PlannedNotification): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION)
            .setData(Uri.parse("nixura://reminder/${Uri.encode(item.key)}"))
            .putExtra(ReminderReceiver.EXTRA_TITLE, item.title)
            .putExtra(ReminderReceiver.EXTRA_BODY, item.body)
            .putExtra(ReminderReceiver.EXTRA_ID, item.key.hashCode())
        return PendingIntent.getBroadcast(appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun encode(items: List<PlannedNotification>): String {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("key", it.key)
                    .put("at", it.triggerAtEpochMillis)
                    .put("title", it.title)
                    .put("body", it.body),
            )
        }
        return array.toString()
    }

    private fun decode(raw: String?): List<PlannedNotification> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map {
                val o = array.getJSONObject(it)
                PlannedNotification(o.getString("key"), o.getLong("at"), o.getString("title"), o.getString("body"))
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS = "nixura_notifications"
        const val KEY_PLAN = "plan"
        const val MAX_SCHEDULED = 60
    }
}
