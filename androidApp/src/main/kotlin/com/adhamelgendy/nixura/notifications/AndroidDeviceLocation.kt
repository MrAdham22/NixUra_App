package com.adhamelgendy.nixura.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.adhamelgendy.nixura.domain.reminder.DeviceLocationGateway
import com.adhamelgendy.nixura.domain.reminder.GeoPoint
import com.adhamelgendy.nixura.domain.reminder.LocationAccess
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * City-level location for prayer times only (`ACCESS_COARSE_LOCATION`, PLAN.md decision 1). One reading from the
 * network provider (no GPS, no Google Play services, no new dependency), asked only from a prayer habit's reminders.
 * [MainActivity] attaches the permission launcher.
 */
class AndroidDeviceLocation(context: Context) : DeviceLocationGateway {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val prefs = appContext.getSharedPreferences("nixura_location_permission", Context.MODE_PRIVATE)

    /** Set by MainActivity: shows the system dialog and calls back with the result. */
    @Volatile
    var launcher: ((onResult: (Boolean) -> Unit) -> Unit)? = null

    /** Set by MainActivity: Android's "show a reason" flag (true after exactly one denial). */
    @Volatile
    var rationale: (() -> Boolean)? = null

    private fun granted() = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun access(): LocationAccess = when {
        granted() -> if (LocationManagerCompat.isLocationEnabled(manager)) LocationAccess.ALLOWED else LocationAccess.OPEN_SETTINGS
        // Never asked, or denied once (Android still shows its dialog); asked before without a rationale = denied twice.
        !prefs.getBoolean(KEY_ASKED, false) || rationale?.invoke() == true -> LocationAccess.ASK
        else -> LocationAccess.OPEN_SETTINGS
    }

    override fun request(onResult: (granted: Boolean) -> Unit) {
        if (granted()) return onResult(true)
        val launch = launcher ?: return onResult(false)
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
        launch(onResult)
    }

    override suspend fun currentCoarse(): GeoPoint? {
        if (!granted()) return null
        val provider = LocationManager.NETWORK_PROVIDER.takeIf { manager.isProviderEnabled(it) }
            ?: LocationManager.PASSIVE_PROVIDER
        val fresh = withTimeoutOrNull(TIMEOUT_MILLIS) { readOnce(provider) }
        @Suppress("MissingPermission") // checked by granted() above
        val location = fresh ?: runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        // City level is enough for prayer times: round to ~1 km so nothing finer is ever stored.
        return location?.let { GeoPoint(round2(it.latitude), round2(it.longitude)) }
    }

    @Suppress("MissingPermission")
    private suspend fun readOnce(provider: String): Location? = suspendCancellableCoroutine { cont ->
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        try {
            LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(appContext)) { location ->
                if (cont.isActive) cont.resume(location)
            }
        } catch (failure: RuntimeException) {
            if (cont.isActive) cont.resume(null)
        }
    }

    override fun openSettings() {
        val intent = if (granted()) {
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) // permission fine, location switched off
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${appContext.packageName}"))
        }
        runCatching { appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

    private companion object {
        const val KEY_ASKED = "asked"
        const val TIMEOUT_MILLIS = 15_000L
    }
}
