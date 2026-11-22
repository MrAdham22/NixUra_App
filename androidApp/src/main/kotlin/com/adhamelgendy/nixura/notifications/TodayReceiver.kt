package com.adhamelgendy.nixura.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.notification.NotificationActionType
import com.adhamelgendy.nixura.domain.notification.NotificationSyncService
import com.adhamelgendy.nixura.domain.reminder.ReminderActionHandler
import com.adhamelgendy.nixura.domain.reminder.ReminderDeviceRepository
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.widgets.WidgetUpdater
import kotlinx.coroutines.CancellationException
import org.koin.core.Koin
import org.koin.core.context.GlobalContext

/**
 * The "Today" notification's broadcasts: its refresh moment, "Hide today" or a swipe (it stays away until tomorrow),
 * and its one log button, which writes through the reminder buttons' handler (same use cases, once per press).
 */
class TodayReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext
        val pending = goAsync()
        WidgetUpdater.launch {
            try {
                val koin = GlobalContext.get()
                val sync = koin.get<NotificationSyncService>()
                val today = koin.get<DayCalculator>().dayIndexOf(koin.get<Clock>().nowEpochMillis())
                val about = intent.getLongExtra(EXTRA_DAY, today)
                when (action) {
                    ACTION_REFRESH -> sync.refreshToday()
                    ACTION_HIDE, ACTION_DISMISSED -> {
                        // The day it was about: swiping away yesterday's does not hide today's.
                        koin.get<ReminderDeviceRepository>().setTodayHiddenDay(about)
                        sync.refreshToday()
                    }
                    ACTION_LOG ->
                        // A button left from yesterday never logs on today: "Isha prayed" would mark all five.
                        if (about != today) {
                            sync.refreshToday()
                        } else {
                            log(koin, intent)
                            WidgetUpdater.requestRefresh(app)
                            sync.syncOnce()
                        }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "today action failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun log(koin: Koin, intent: Intent) {
        val name = intent.getStringExtra(EXTRA_TYPE)
        val type = NotificationActionType.entries.firstOrNull { it.name == name } ?: return
        val habit = intent.getLongExtra(EXTRA_HABIT, -1L).takeIf { it > 0 } ?: return
        val prayer = intent.getIntExtra(EXTRA_PRAYER, -1).takeIf { it > 0 }
        val token = ReminderActionHandler.tokenOf("today:$habit", intent.getLongExtra(EXTRA_POSTED_AT, 0L), type)
        koin.get<ReminderActionHandler>().handle(token, type, habit, prayer)
    }

    companion object {
        const val ACTION_REFRESH = "com.adhamelgendy.nixura.TODAY_REFRESH"
        const val ACTION_HIDE = "com.adhamelgendy.nixura.TODAY_HIDE"
        const val ACTION_DISMISSED = "com.adhamelgendy.nixura.TODAY_DISMISSED"
        const val ACTION_LOG = "com.adhamelgendy.nixura.TODAY_LOG"
        const val EXTRA_DAY = "day"
        const val EXTRA_POSTED_AT = "posted"
        const val EXTRA_TYPE = "type"
        const val EXTRA_HABIT = "habit"
        const val EXTRA_PRAYER = "prayer"
        private const val TAG = "NixUraToday"
    }
}
