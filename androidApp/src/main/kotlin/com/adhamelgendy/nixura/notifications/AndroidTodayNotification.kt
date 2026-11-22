package com.adhamelgendy.nixura.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.adhamelgendy.nixura.R
import com.adhamelgendy.nixura.domain.notification.NotificationActionType
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.notification.TodayPresenter
import com.adhamelgendy.nixura.domain.notification.TodayState
import com.adhamelgendy.nixura.domain.notification.TodaySummary
import com.adhamelgendy.nixura.presentation.locale.Digits

/**
 * The opt-in "Today" notification (phase D): ongoing, silent, no foreground service and no Live Update request.
 *
 * It is not pinned: since Android 14 the user can swipe it away while unlocked, and then it stays away for the day
 * ([TodayReceiver.ACTION_DISMISSED]). It is posted again only when what it shows changed or when it is missing
 * (reboot, app update). One alarm redraws it at [TodaySummary.refreshAtEpochMillis]: a 10-minute window and never a
 * wake-up, because nobody reads it while the screen is off.
 */
class AndroidTodayNotification(context: Context) : TodayPresenter {
    private val appContext = context.applicationContext
    private val alarms = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun show(state: TodayState): Unit = synchronized(LOCK) {
        when (state) {
            TodayState.Off -> {
                remove()
                alarms.cancel(refreshIntent())
            }
            is TodayState.Hidden -> {
                remove()
                refreshAt(state.refreshAtEpochMillis)
            }
            is TodayState.Showing -> {
                refreshAt(state.summary.refreshAtEpochMillis)
                post(state.summary)
            }
        }
    }

    private fun remove() {
        NixNotifications.cancel(appContext, KEY)
        prefs.edit().remove(KEY_SHOWN).apply()
    }

    private fun post(summary: TodaySummary) {
        val shown = summary.toString().hashCode()
        if (prefs.getInt(KEY_SHOWN, 0) == shown && isShowing()) return
        fun digits(text: String) = Digits.localize(text, summary.digitStyle)
        // A fresh token per posting: a press counts once, and the next posting's button counts again.
        val postedAt = System.currentTimeMillis()
        val log = summary.action?.let { action ->
            val intent = intent(TodayReceiver.ACTION_LOG, "log/${summary.day}/$postedAt")
                .putExtra(TodayReceiver.EXTRA_DAY, summary.day)
                .putExtra(TodayReceiver.EXTRA_POSTED_AT, postedAt)
                .putExtra(TodayReceiver.EXTRA_TYPE, action.type.name)
                .putExtra(TodayReceiver.EXTRA_HABIT, action.habitId)
                .apply { action.prayerIndex?.let { putExtra(TodayReceiver.EXTRA_PRAYER, it) } }
            val icon = if (action.type == NotificationActionType.LOG_PRAYER) R.drawable.ic_w_check else R.drawable.ic_w_plus
            NotificationCompat.Action.Builder(icon, digits(action.label), broadcast(intent)).build()
        }
        val dayIntent = { action: String, path: String -> broadcast(intent(action, path).putExtra(TodayReceiver.EXTRA_DAY, summary.day)) }
        val hide = NotificationCompat.Action.Builder(R.drawable.ic_w_minus, summary.hideLabel, dayIntent(TodayReceiver.ACTION_HIDE, "hide")).build()
        NixNotifications.post(
            appContext,
            NixNotificationRequest(
                key = KEY,
                kind = NotificationKind.TODAY,
                title = digits(summary.title),
                body = digits(summary.text),
                expandedText = digits(summary.lines.joinToString("\n")),
                actions = listOfNotNull(log, hide),
                onlyAlertOnce = true,
                ongoing = true,
                countdownTo = summary.countdown?.atEpochMillis,
                countdownLabel = summary.countdown?.metricLabel,
                countdownClock = summary.countdown?.clock?.let { it.copy(time = digits(it.time)) },
                metrics = summary.metrics.map { it.copy(value = digits(it.value)) },
                deleteIntent = dayIntent(TodayReceiver.ACTION_DISMISSED, "dismissed"),
            ),
        )
        prefs.edit().putInt(KEY_SHOWN, shown).apply()
    }

    private fun isShowing(): Boolean {
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return false
        val id = NixNotifications.idOf(KEY)
        return runCatching { manager.activeNotifications.any { it.id == id } }.getOrDefault(false)
    }

    private fun refreshAt(at: Long) {
        alarms.setWindow(AlarmManager.RTC, at, REFRESH_WINDOW_MILLIS, refreshIntent()) // RTC, not RTC_WAKEUP
    }

    private fun refreshIntent(): PendingIntent = broadcast(intent(TodayReceiver.ACTION_REFRESH, "refresh"))

    private fun intent(action: String, path: String): Intent =
        Intent(appContext, TodayReceiver::class.java).setAction(action).setData(Uri.parse("nixura://today/$path"))

    private fun broadcast(intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private companion object {
        const val KEY = "today"
        const val PREFS = "nixura_today"
        const val KEY_SHOWN = "shown"

        /** Android 12+ allows no shorter window. */
        const val REFRESH_WINDOW_MILLIS = 10 * 60_000L

        val LOCK = Any()
    }
}
