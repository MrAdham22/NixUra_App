package com.adhamelgendy.nixura.notifications

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.adhamelgendy.nixura.domain.notification.NotificationPermissionGateway
import com.adhamelgendy.nixura.domain.notification.ReminderCapabilities

/**
 * Bridges the shared UI to the Android notification permission (API 33+), to the "notifications switched off"
 * state and to the exact-alarm permission (API 31+). [MainActivity] attaches its activity-result launcher on
 * every create, so this survives configuration changes.
 */
class AndroidNotificationPermission(context: Context) : NotificationPermissionGateway, ReminderCapabilities {
    private val appContext = context.applicationContext

    @Volatile
    var launcher: (() -> Unit)? = null

    override fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun request() {
        if (!isGranted()) launcher?.invoke()
    }

    override fun notificationsAllowed(): Boolean = isGranted() && NotificationManagerCompat.from(appContext).areNotificationsEnabled()

    override fun exactAlarmsAllowed(): Boolean = canScheduleExactAlarms(appContext)

    override fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
        startSettings(intent, fallback = appDetailsIntent())
    }

    override fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < 31) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${appContext.packageName}"))
        startSettings(intent, fallback = appDetailsIntent())
    }

    private fun appDetailsIntent() =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${appContext.packageName}"))

    /** Starts a system settings screen from outside an activity; falls back to the app-details screen if the OEM lacks it. */
    private fun startSettings(intent: Intent, fallback: Intent) {
        val launch = { target: Intent -> appContext.startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        try {
            launch(intent)
        } catch (failure: RuntimeException) {
            try { launch(fallback) } catch (failure: RuntimeException) { /* nothing left to open; the user can use system Settings */ }
        }
    }

    companion object {
        /** Exact alarms need a user-granted permission from API 31 (and it can be revoked); older versions always allow them. */
        fun canScheduleExactAlarms(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < 31) return true
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarms.canScheduleExactAlarms()
        }
    }
}
