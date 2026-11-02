package com.example.reclaimapp.data.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sends signed-in requests and renews the access token transparently (the multiplatform equivalent of an
 * OkHttp Authenticator).
 *
 * Single flight: when several requests get "session expired" at once, exactly one calls /refresh while the
 * others wait on the lock, then reuse the new token (refresh tokens rotate, so two parallel refreshes would
 * trip the server's reuse detection and sign the user out). If the server says the session is over, the
 * user is signed out locally (clean logout); if only the network failed the session is kept.
 */
internal class AuthorizedApi(
    private val transport: ApiTransport,
    private val sessions: SessionManagerImpl,
) {
    private val refreshMutex = Mutex()

    suspend fun call(method: String, path: String, body: String? = null): TransportResponse {
        val session = sessions.current() ?: return TransportResponse(401, SESSION_MISSING_BODY)
        val first = transport.execute(method, path, body, session.accessToken)
        if (!needsRefresh(first)) return first
        return when (val outcome = refresh(session.accessToken)) {
            is Refresh.Done -> transport.execute(method, path, body, outcome.accessToken)
            Refresh.Ended -> TransportResponse(401, SESSION_MISSING_BODY)
            Refresh.Unavailable -> TransportResponse(0)
        }
    }

    /** Only "your token is no good" answers trigger a refresh; a wrong password (also 401) must not. */
    private fun needsRefresh(r: TransportResponse): Boolean =
        r.status == 401 && AuthErrorMapper.codeOf(r.body).let { it == null || it == "unauthorized" || it == "session_revoked" }

    private sealed interface Refresh {
        data class Done(val accessToken: String) : Refresh
        data object Ended : Refresh
        data object Unavailable : Refresh
    }

    private suspend fun refresh(failedAccessToken: String): Refresh = refreshMutex.withLock {
        val current = sessions.current() ?: return@withLock Refresh.Ended
        // Another request already renewed the tokens while this one waited for the lock.
        if (current.accessToken != failedAccessToken) return@withLock Refresh.Done(current.accessToken)
        val res = transport.execute(
            "POST", "/v1/auth/refresh",
            AuthJson.encodeToString(RefreshRequest.serializer(), RefreshRequest(current.refreshToken)), null,
        )
        when {
            res.status == 200 -> {
                val tokens = runCatching { AuthJson.decodeFromString(TokensDto.serializer(), res.body) }.getOrNull()
                    ?: return@withLock Refresh.Unavailable
                sessions.replaceTokens(current.sessionId, tokens.accessToken, tokens.refreshToken)
                Refresh.Done(tokens.accessToken)
            }
            res.status == 401 -> {
                sessions.signOutLocally()
                Refresh.Ended
            }
            else -> Refresh.Unavailable
        }
    }

    private companion object {
        const val SESSION_MISSING_BODY = """{"error":{"code":"unauthorized","message":"Please sign in again."}}"""
    }
}
