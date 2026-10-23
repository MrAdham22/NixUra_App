package com.example.reclaimapp.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.reclaimapp.domain.notification.NotificationPermissionGateway

/**
 * Bridges the shared UI to the Android runtime permission (API 33+). [MainActivity] attaches its
 * activity-result launcher on every create, so this survives configuration changes.
 */
class AndroidNotificationPermission(context: Context) : NotificationPermissionGateway {
    private val appContext = context.applicationContext

    @Volatile
    var launcher: (() -> Unit)? = null

    override fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun request() {
        if (!isGranted()) launcher?.invoke()
    }
}
