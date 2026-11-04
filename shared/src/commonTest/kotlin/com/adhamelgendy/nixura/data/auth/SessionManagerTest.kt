package com.adhamelgendy.nixura.data.auth

import com.adhamelgendy.nixura.domain.auth.GuestDataHandoff
import com.adhamelgendy.nixura.domain.auth.SessionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionManagerTest {

    @Test fun startsLoadingThenRestoresStoredSession() = runTest {
        val store = InMemorySecureStore()
        SessionManagerImpl(store).signIn(storedSession())
        val fresh = SessionManagerImpl(store)
        assertEquals(SessionState.Loading, fresh.state.value)
        fresh.restore()
        assertEquals("u1", (fresh.state.value as SessionState.SignedIn).user.id)
        assertEquals("u1", fresh.currentUserId.first())
    }

    @Test fun restoreWithNothingStoredIsSignedOut() = runTest {
        val m = SessionManagerImpl(InMemorySecureStore())
        m.restore()
        assertEquals(SessionState.SignedOut, m.state.value)
        assertNull(m.currentUserId.first())
    }

    @Test fun corruptStoredSessionIsTreatedAsSignedOut() = runTest {
        val store = InMemorySecureStore().apply { map["session"] = "{not json" }
        val m = SessionManagerImpl(store)
        m.restore()
        assertEquals(SessionState.SignedOut, m.state.value)
    }

    @Test fun firstEverSignInMarksGuestDataForAdoption() = runTest {
        val m = SessionManagerImpl(InMemorySecureStore())
        m.signIn(storedSession())
        assertEquals(GuestDataHandoff("u1", null), m.pendingGuestHandoff())
        m.markGuestHandoffHandled()
        assertNull(m.pendingGuestHandoff())
    }

    @Test fun sameUserSigningBackInHasNothingPending() = runTest {
        val m = SessionManagerImpl(InMemorySecureStore())
        m.signIn(storedSession())
        m.markGuestHandoffHandled()
        m.signOutLocally()
        m.signIn(storedSession(access = "a2"))
        assertNull(m.pendingGuestHandoff())
    }

    @Test fun differentUserSigningInReportsThePreviousAccount() = runTest {
        val m = SessionManagerImpl(InMemorySecureStore())
        m.signIn(storedSession(userId = "u1"))
        m.markGuestHandoffHandled()
        m.signOutLocally()
        m.signIn(storedSession(userId = "u2"))
        assertEquals(GuestDataHandoff("u2", "u1"), m.pendingGuestHandoff())
    }

    @Test fun signOutClearsTokensButNotTheHandoffFlag() = runTest {
        val store = InMemorySecureStore()
        val m = SessionManagerImpl(store)
        m.signIn(storedSession())
        m.signOutLocally()
        assertFalse("session" in store.map)
        assertIs<SessionState.SignedOut>(m.state.value)
        assertTrue(m.pendingGuestHandoff() != null)
    }

    @Test fun replaceTokensIgnoresAnotherSession() = runTest {
        val m = SessionManagerImpl(InMemorySecureStore())
        m.signIn(storedSession())
        m.replaceTokens("other-session", "x", "y")
        assertEquals("old", m.current()?.accessToken)
        m.replaceTokens("s1", "a", "b")
        assertEquals("a", m.current()?.accessToken)
    }
}
