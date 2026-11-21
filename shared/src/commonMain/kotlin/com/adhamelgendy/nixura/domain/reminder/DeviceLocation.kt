package com.adhamelgendy.nixura.domain.reminder

/** Whether the app may read a city-level location now, and what the UI should offer if not. */
enum class LocationAccess {
    ALLOWED,

    /** Not allowed yet, the system dialog can still be shown: explain first (one line), then ask. */
    ASK,

    /** Denied for good (Android won't ask again) or location is off: offer the system settings. */
    OPEN_SETTINGS,

    /** This platform has no location support in the app yet (iOS until CLLocationManager is wired). */
    UNAVAILABLE,
}

/**
 * One coarse reading of where the phone is, for prayer times only (PLAN.md decision 1): asked only from a prayer
 * habit's reminder controls, never at launch, never in the background, never for another habit.
 */
interface DeviceLocationGateway {
    fun access(): LocationAccess

    /** Shows the system permission dialog; [onResult] gets true when city-level location was granted. */
    fun request(onResult: (granted: Boolean) -> Unit)

    /** One coarse reading, or `null` when the phone could not tell (location switched off, no fix in time). */
    suspend fun currentCoarse(): GeoPoint?

    fun openSettings()
}

/** Platforms without location support yet: prayer reminders explain that they are not available here. */
object UnavailableDeviceLocation : DeviceLocationGateway {
    override fun access(): LocationAccess = LocationAccess.UNAVAILABLE
    override fun request(onResult: (granted: Boolean) -> Unit) = onResult(false)
    override suspend fun currentCoarse(): GeoPoint? = null
    override fun openSettings() = Unit
}
