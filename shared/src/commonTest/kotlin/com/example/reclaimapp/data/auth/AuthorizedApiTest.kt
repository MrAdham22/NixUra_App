package com.example.reclaimapp.data.auth

import com.example.reclaimapp.domain.auth.SessionState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthorizedApiTest {

    private suspend fun signedIn(store: InMemorySecureStore = InMemorySecureStore()): SessionManagerImpl =
        SessionManagerImpl(store).also { it.signIn(storedSession()) }

    @Test fun successfulCallDoesNotRefresh() = runTest {
        val sessions = signedIn()
        val t = FakeTransport { _, _, _ -> TransportResponse(200, "{}") }
        assertEquals(200, AuthorizedApi(t, sessions).call("GET", "/v1/me").status)
        assertEquals(0, t.count("/v1/auth/refresh"))
    }

    @Test fun expiredTokenIsRefreshedOnceAndRequestRetried() = runTest {
        val sessions = signedIn()
        val t = FakeTransport { _, path, bearer ->
            when {
                path == "/v1/auth/refresh" -> TransportResponse(200, REFRESH_OK)
                bearer == "old" -> TransportResponse(401, UNAUTHORIZED)
                else -> TransportResponse(200, "{}")
            }
        }
        val r = AuthorizedApi(t, sessions).call("GET", "/v1/me")
        assertEquals(200, r.status)
        assertEquals("new", sessions.current()?.accessToken)
        assertEquals("r2", sessions.current()?.refreshToken)
    }

    @Test fun concurrentExpiredRequestsShareOneRefresh() = runTest {
        val sessions = signedIn()
        val t = FakeTransport { _, path, bearer ->
            when {
                path == "/v1/auth/refresh" -> { delay(50); TransportResponse(200, REFRESH_OK) }
                bearer == "old" -> TransportResponse(401, UNAUTHORIZED)
                else -> TransportResponse(200, "{}")
            }
        }
        val api = AuthorizedApi(t, sessions)
        val results = (1..5).map { async { api.call("GET", "/v1/me") } }.awaitAll()
        assertTrue(results.all { it.status == 200 })
        assertEquals(1, t.count("/v1/auth/refresh"), "refresh tokens rotate: parallel refreshes would trigger reuse detection")
    }

    @Test fun wrongPasswordUnauthorizedDoesNotTriggerRefresh() = runTest {
        val sessions = signedIn()
        val t = FakeTransport { _, _, _ -> TransportResponse(401, """{"error":{"code":"invalid_credentials","message":"x"}}""") }
        assertEquals(401, AuthorizedApi(t, sessions).call("POST", "/v1/auth/change-password", "{}").status)
        assertEquals(0, t.count("/v1/auth/refresh"))
        assertIs<SessionState.SignedIn>(sessions.state.value)
    }

    @Test fun refreshRejectedSignsOutCleanly() = runTest {
        val sessions = signedIn()
        val t = FakeTransport { _, path, _ ->
            if (path == "/v1/auth/refresh") TransportResponse(401, UNAUTHORIZED) else TransportResponse(401, UNAUTHORIZED)
        }
        val r = AuthorizedApi(t, sessions).call("GET", "/v1/me")
        assertEquals(401, r.status)
        assertEquals(SessionState.SignedOut, sessions.state.value)
        assertEquals(null, sessions.current())
    }

    @Test fun networkFailureDuringRefreshKeepsTheSession() = runTest {
        val sessions = signedIn()
        val t = FakeTransport { _, path, _ ->
            if (path == "/v1/auth/refresh") TransportResponse(0) else TransportResponse(401, UNAUTHORIZED)
        }
        val r = AuthorizedApi(t, sessions).call("GET", "/v1/me")
        assertTrue(r.isNetworkFailure)
        assertIs<SessionState.SignedIn>(sessions.state.value)
        assertEquals("old", sessions.current()?.accessToken)
    }

    @Test fun noSessionMeansUnauthorizedWithoutNetwork() = runTest {
        val sessions = SessionManagerImpl(InMemorySecureStore())
        val t = FakeTransport { _, _, _ -> TransportResponse(200) }
        assertEquals(401, AuthorizedApi(t, sessions).call("GET", "/v1/me").status)
        assertTrue(t.calls.isEmpty())
    }
}
