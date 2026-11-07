package com.adhamelgendy.nixura.data.auth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthorizedApiTest {

    private val clock = FixedClock()

    private suspend fun setup(firebase: FakeFirebase, expiresAt: Long = clock.now + 3_600_000): SessionManagerImpl =
        SessionManagerImpl(InMemorySecureStore()).also { it.signIn(storedSession(expiresAt = expiresAt)) }

    private fun api(t: ApiTransport, sessions: SessionManagerImpl, firebase: FakeFirebase) =
        AuthorizedApi(t, FirebaseSession(FirebaseAuthApi(FirebaseConfig("k"), firebase), sessions, clock))

    @Test fun sendsTheCurrentIdToken() = runTest {
        val firebase = FakeFirebase { _, _ -> error("no refresh expected") }
        val sessions = setup(firebase)
        val t = FakeTransport { _, _, bearer -> TransportResponse(if (bearer == "old") 200 else 401) }
        assertEquals(200, api(t, sessions, firebase).call("GET", "/v1/me").status)
    }

    @Test fun expiredTokenIsRenewedBeforeTheCall() = runTest {
        val firebase = FakeFirebase { m, _ -> if (m == "token") TransportResponse(200, REFRESH_OK) else error(m) }
        val sessions = setup(firebase, expiresAt = clock.now - 1)
        val t = FakeTransport { _, _, bearer -> TransportResponse(if (bearer == "new") 200 else 401) }
        assertEquals(200, api(t, sessions, firebase).call("GET", "/v1/me").status)
        assertEquals("new", sessions.current()?.accessToken)
        assertEquals("r2", sessions.current()?.refreshToken)
    }

    @Test fun a401IsRetriedOnceWithARenewedTokenButNeverSignsOut() = runTest {
        val firebase = FakeFirebase { m, _ -> if (m == "token") TransportResponse(200, REFRESH_OK) else error(m) }
        val sessions = setup(firebase)
        val t = FakeTransport { _, _, _ -> TransportResponse(401) }
        assertEquals(401, api(t, sessions, firebase).call("GET", "/v1/me").status)
        assertEquals(2, t.calls.size)
        assertIs<StoredSession>(sessions.current())
    }

    @Test fun offlineRefreshKeepsTheSession() = runTest {
        val firebase = FakeFirebase { _, _ -> TransportResponse(0) }
        val sessions = setup(firebase, expiresAt = 0)
        val t = FakeTransport { _, _, _ -> error("must not call the server without a token") }
        assertEquals(0, api(t, sessions, firebase).call("GET", "/v1/me").status)
        assertIs<StoredSession>(sessions.current())
    }

    @Test fun revokedRefreshTokenSignsOutLocally() = runTest {
        val firebase = FakeFirebase { _, _ -> fbError("INVALID_REFRESH_TOKEN") }
        val sessions = setup(firebase, expiresAt = 0)
        val t = FakeTransport { _, _, _ -> error("must not call the server") }
        assertEquals(401, api(t, sessions, firebase).call("GET", "/v1/me").status)
        assertEquals(null, sessions.current())
    }

    @Test fun signedOutReturns401WithoutAnyCall() = runTest {
        val firebase = FakeFirebase { _, _ -> error("no call") }
        val sessions = SessionManagerImpl(InMemorySecureStore())
        val t = FakeTransport { _, _, _ -> error("no call") }
        assertEquals(401, api(t, sessions, firebase).call("GET", "/v1/me").status)
    }
}
