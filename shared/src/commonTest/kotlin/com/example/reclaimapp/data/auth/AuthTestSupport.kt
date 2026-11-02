package com.example.reclaimapp.data.auth

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

internal fun storedSession(access: String = "old", refresh: String = "r1", userId: String = "u1") =
    StoredSession(access, refresh, "s1", StoredUser(userId, "ann@example.com", "Ann"))

const val UNAUTHORIZED = """{"error":{"code":"unauthorized","message":"x"}}"""
const val REFRESH_OK = """{"accessToken":"new","accessTokenExpiresInSeconds":900,"refreshToken":"r2","sessionId":"s1"}"""
