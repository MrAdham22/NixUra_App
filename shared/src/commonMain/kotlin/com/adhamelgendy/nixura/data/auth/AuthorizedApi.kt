package com.adhamelgendy.nixura.data.auth

/**
 * Sends signed-in requests to NixUra's own server with the user's Firebase ID token. A 401 gets one retry with a
 * freshly renewed token; it never signs the user out by itself (only Firebase decides that, in [FirebaseSession]).
 */
internal class AuthorizedApi(
    private val transport: ApiTransport,
    private val tokens: FirebaseSession,
) {
    suspend fun call(method: String, path: String, body: String? = null): TransportResponse {
        val first = when (val t = tokens.idToken()) {
            is IdTokenResult.Ok -> transport.execute(method, path, body, t.idToken)
            IdTokenResult.Ended -> return TransportResponse(401, SESSION_MISSING_BODY)
            IdTokenResult.Offline -> return TransportResponse(0)
        }
        if (first.status != 401) return first
        return when (val t = tokens.idToken(forceRefresh = true)) {
            is IdTokenResult.Ok -> transport.execute(method, path, body, t.idToken)
            IdTokenResult.Ended -> TransportResponse(401, SESSION_MISSING_BODY)
            IdTokenResult.Offline -> TransportResponse(0)
        }
    }

    private companion object {
        const val SESSION_MISSING_BODY = """{"error":{"code":"unauthorized","message":"Please sign in again."}}"""
    }
}
