package com.adhamelgendy.nixura.data.auth

import com.adhamelgendy.nixura.domain.clock.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A usable ID token, a session that is over (the user was signed out locally), or no network to renew it. */
internal sealed interface IdTokenResult {
    data class Ok(val idToken: String) : IdTokenResult
    data object Ended : IdTokenResult
    data object Offline : IdTokenResult
}

/**
 * Hands out the signed-in user's Firebase ID token and renews it shortly before it expires (they last an hour).
 *
 * Single flight: when several callers need a new token at once, exactly one asks Firebase while the others wait on the
 * lock and then reuse it. If Firebase says the refresh token is no good (account deleted or disabled, password changed
 * on another phone), the user is signed out locally; if only the network failed, the session is kept.
 */
internal class FirebaseSession(
    private val api: FirebaseAuthApi,
    private val sessions: SessionManagerImpl,
    private val clock: Clock,
) {
    private val refreshMutex = Mutex()

    suspend fun idToken(forceRefresh: Boolean = false): IdTokenResult {
        val seen = sessions.current() ?: return IdTokenResult.Ended
        if (!forceRefresh && isFresh(seen)) return IdTokenResult.Ok(seen.accessToken)
        return refreshMutex.withLock {
            val current = sessions.current() ?: return@withLock IdTokenResult.Ended
            // Another caller already renewed it while this one waited for the lock.
            if (current.accessToken != seen.accessToken && isFresh(current)) return@withLock IdTokenResult.Ok(current.accessToken)
            when (val r = api.refresh(current.refreshToken)) {
                is FbResult.Ok -> {
                    val expiresAt = expiryOf(r.value.expiresIn)
                    sessions.replaceTokens(current.sessionId, r.value.idToken, r.value.refreshToken, expiresAt)
                    IdTokenResult.Ok(r.value.idToken)
                }
                is FbResult.Err -> if (r.code in SESSION_OVER) {
                    sessions.signOutLocally()
                    IdTokenResult.Ended
                } else {
                    IdTokenResult.Offline
                }
                FbResult.Offline -> IdTokenResult.Offline
            }
        }
    }

    /** Epoch millis at which a token valid for [expiresInSeconds] (Firebase sends it as a string) runs out. */
    fun expiryOf(expiresInSeconds: String): Long =
        clock.nowEpochMillis() + (expiresInSeconds.toLongOrNull() ?: DEFAULT_LIFETIME_SECONDS) * 1_000

    private fun isFresh(s: StoredSession) = s.expiresAt - EARLY_REFRESH_MILLIS > clock.nowEpochMillis()

    private companion object {
        const val DEFAULT_LIFETIME_SECONDS = 3_600L
        const val EARLY_REFRESH_MILLIS = 5 * 60_000L

        /** Refresh answers that mean the session is over for good. */
        val SESSION_OVER = setOf("TOKEN_EXPIRED", "USER_DISABLED", "USER_NOT_FOUND", "INVALID_REFRESH_TOKEN", "INVALID_GRANT_TYPE", "MISSING_REFRESH_TOKEN")
    }
}
