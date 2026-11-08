package com.adhamelgendy.nixura.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Alarms are cleared on reboot and app update; re-arm the stored plan. Also re-arms when the exact-alarm
 * permission is granted or revoked, so reminders switch between exact and inexact without opening the app.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == ACTION_EXACT_ALARM_PERMISSION_CHANGED
        ) {
            AndroidNotificationScheduler(context).restoreFromStorage()
        }
    }

    private companion object {
        const val ACTION_EXACT_ALARM_PERMISSION_CHANGED = "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
