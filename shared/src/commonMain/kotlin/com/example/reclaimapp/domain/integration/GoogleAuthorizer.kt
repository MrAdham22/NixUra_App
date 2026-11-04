package com.example.reclaimapp.domain.integration

/** The result of asking for a Google Calendar (read-only) access token. */
sealed interface GoogleAuthResult {
    /** A short-lived token; it is used for one sync and never stored. */
    data class Granted(val accessToken: String, val accountLabel: String = "") : GoogleAuthResult

    /** The user has to approve access in Google's own screen (only possible while the app is open). */
    data object NeedsConsent : GoogleAuthResult

    /** The user closed Google's consent screen without choosing. */
    data object Cancelled : GoogleAuthResult

    /** Google refused or failed (the status code is logged on the device, never shown). */
    data object Denied : GoogleAuthResult
    data object Offline : GoogleAuthResult

    /** This build has no Google OAuth client set up, or the platform does not support it yet (iOS). */
    data object NotAvailable : GoogleAuthResult
}

/**
 * Platform port for Google's authorization (Android: Google Identity Authorization API, minimal scope
 * calendar.readonly). [interactive] = may show Google's consent screen; background syncs always pass false.
 */
interface GoogleAuthorizer {
    suspend fun authorize(interactive: Boolean): GoogleAuthResult

    /** Revokes the grant so NixUra loses access; safe to call when not granted. */
    suspend fun revoke()

    /** Drops a token Google rejected (expired or revoked) so the next [authorize] fetches a fresh one. */
    suspend fun forget(accessToken: String)
}
