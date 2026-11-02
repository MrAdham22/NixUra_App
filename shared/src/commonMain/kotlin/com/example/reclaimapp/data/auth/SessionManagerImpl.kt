package com.example.reclaimapp.data.auth

import com.example.reclaimapp.domain.auth.AuthUser
import com.example.reclaimapp.domain.auth.GuestDataHandoff
import com.example.reclaimapp.domain.auth.SessionManager
import com.example.reclaimapp.domain.auth.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Holds the signed-in session in memory ([state]) and persists it in the [SecureStore]. */
class SessionManagerImpl internal constructor(private val store: SecureStore) : SessionManager {

    private val mutex = Mutex()
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    override val state: StateFlow<SessionState> = _state.asStateFlow()
    override val currentUserId: Flow<String?> =
        _state.map { (it as? SessionState.SignedIn)?.user?.id }.distinctUntilChanged()

    /** Reads the stored session once at app start. Safe to call repeatedly. */
    suspend fun restore() = mutex.withLock {
        val session = read()
        _state.value = if (session == null) SessionState.SignedOut else SessionState.SignedIn(session.user.toAuthUser())
    }

    internal suspend fun current(): StoredSession? = mutex.withLock { read() }

    internal suspend fun signIn(session: StoredSession) = mutex.withLock {
        val last = store.get(KEY_LAST_USER)
        store.put(KEY_SESSION, AuthJson.encodeToString(StoredSession.serializer(), session))
        // A different account (or the first ever) is now on this device: the sync layer must adopt or clear local data.
        if (last != session.user.id) store.put(KEY_PENDING, session.user.id + SEP + (last ?: ""))
        store.put(KEY_LAST_USER, session.user.id)
        store.remove(KEY_PROMPT_SEEN) // a later sign-out shows the Welcome screen again
        _state.value = SessionState.SignedIn(session.user.toAuthUser())
    }

    /** Swaps tokens after a refresh, only if the stored session is still the same one. */
    internal suspend fun replaceTokens(sessionId: String, accessToken: String, refreshToken: String) = mutex.withLock {
        val s = read() ?: return@withLock
        if (s.sessionId != sessionId) return@withLock
        store.put(KEY_SESSION, AuthJson.encodeToString(StoredSession.serializer(), s.copy(accessToken = accessToken, refreshToken = refreshToken)))
    }

    internal suspend fun updateUser(user: AuthUser) = mutex.withLock {
        val s = read() ?: return@withLock
        val stored = StoredUser(user.id, user.email, user.name, user.age, user.emailVerified)
        store.put(KEY_SESSION, AuthJson.encodeToString(StoredSession.serializer(), s.copy(user = stored)))
        _state.value = SessionState.SignedIn(user)
    }

    override suspend fun signOutLocally() = mutex.withLock {
        store.remove(KEY_SESSION)
        _state.value = SessionState.SignedOut
    }

    override suspend fun pendingGuestHandoff(): GuestDataHandoff? = mutex.withLock {
        val raw = store.get(KEY_PENDING) ?: return@withLock null
        val user = raw.substringBefore(SEP)
        val previous = raw.substringAfter(SEP, "").ifEmpty { null }
        if (user.isEmpty()) null else GuestDataHandoff(user, previous)
    }

    override suspend fun markGuestHandoffHandled() = mutex.withLock { store.remove(KEY_PENDING) }

    /** True once the first-launch "create an account?" screen was shown or skipped. */
    suspend fun authPromptSeen(): Boolean = store.get(KEY_PROMPT_SEEN) == "1"
    suspend fun markAuthPromptSeen() = store.put(KEY_PROMPT_SEEN, "1")

    private suspend fun read(): StoredSession? {
        val raw = store.get(KEY_SESSION) ?: return null
        return runCatching { AuthJson.decodeFromString(StoredSession.serializer(), raw) }.getOrNull()
    }

    private fun StoredUser.toAuthUser() = AuthUser(id, email, name, age, emailVerified)

    private companion object {
        const val KEY_SESSION = "session"
        const val KEY_LAST_USER = "last_user_id"
        const val KEY_PENDING = "guest_handoff"
        const val KEY_PROMPT_SEEN = "auth_prompt_seen"
        const val SEP = "|"
    }
}
