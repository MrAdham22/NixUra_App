package com.adhamelgendy.nixura.data.auth

class InMemorySecureStore : SecureStore {
    val map = mutableMapOf<String, String>()
    override suspend fun get(key: String) = map[key]
    override suspend fun put(key: String, value: String) { map[key] = value }
    override suspend fun remove(key: String) { map.remove(key) }
}

/** Scriptable transport that records every call. */
class FakeTransport(private val handler: suspend (method: String, path: String, bearer: String?) -> TransportResponse) : ApiTransport {
    val calls = mutableListOf<Triple<String, String, String?>>()
    override suspend fun execute(method: String, path: String, body: String?, bearer: String?): TransportResponse {
        calls += Triple(method, path, bearer)
        return handler(method, path, bearer)
    }
    fun count(path: String) = calls.count { it.second == path }
}

internal fun storedSession(access: String = "old", refresh: String = "r1", userId: String = "u1", expiresAt: Long = 0) =
    StoredSession(access, refresh, "s1", StoredUser(userId, "ann@example.com", "Ann"), expiresAt)

/** Scriptable Firebase REST endpoint: [handler] gets the method name ("accounts:signUp", "token") and the body. */
internal class FakeFirebase(private val handler: (method: String, body: String) -> TransportResponse) : FirebaseTransport {
    val calls = mutableListOf<Pair<String, String>>()
    override suspend fun post(url: String, body: String, form: Boolean): TransportResponse {
        val method = url.substringAfterLast('/').substringBefore('?')
        calls += method to body
        return handler(method, body)
    }
    fun count(method: String) = calls.count { it.first == method }
}

internal class FixedClock(var now: Long = 1_000_000L) : com.adhamelgendy.nixura.domain.clock.Clock {
    override fun nowEpochMillis(): Long = now
}

internal fun fbError(message: String, status: Int = 400) = TransportResponse(status, """{"error":{"code":$status,"message":"$message"}}""")
internal const val REFRESH_OK = """{"id_token":"new","refresh_token":"r2","expires_in":"3600","user_id":"u1"}"""
