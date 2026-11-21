package com.adhamelgendy.nixura.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adhamelgendy.nixura.domain.notification.NotificationSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Alarms are cleared on reboot and app update; re-arm the stored plan. Also re-arms when the exact-alarm
 * permission is granted or revoked, so reminders switch between exact and inexact without opening the app.
 *
 * A time-zone or clock change re-plans instead: the stored trigger times were computed for the old local time
 * (the daily check-in at 08:30 would otherwise fire at the wrong hour).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_EXACT_ALARM_PERMISSION_CHANGED,
            -> AndroidNotificationScheduler(context).restoreFromStorage()

            // An app update may change how reminders are planned (phase C added good-habit reminders): plan afresh.
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> replan(context)
        }
    }

    private fun replan(context: Context) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val service = GlobalContext.getOrNull()?.getOrNull<NotificationSyncService>()
                if (service != null) service.syncOnce() else AndroidNotificationScheduler(context).restoreFromStorage()
            } catch (failure: Exception) {
                AndroidNotificationScheduler(context).restoreFromStorage()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val ACTION_EXACT_ALARM_PERMISSION_CHANGED = "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
