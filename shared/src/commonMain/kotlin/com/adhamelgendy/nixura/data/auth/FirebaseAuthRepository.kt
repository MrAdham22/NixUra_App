package com.adhamelgendy.nixura.data.auth

import com.adhamelgendy.nixura.domain.auth.AuthError
import com.adhamelgendy.nixura.domain.auth.AuthErrorKind
import com.adhamelgendy.nixura.domain.auth.AuthRepository
import com.adhamelgendy.nixura.domain.auth.AuthResult
import com.adhamelgendy.nixura.domain.auth.AuthUser
import com.adhamelgendy.nixura.domain.auth.SocialProvider
import com.adhamelgendy.nixura.domain.auth.SocialSignIn
import com.adhamelgendy.nixura.domain.auth.SocialToken
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Accounts on Firebase Authentication (email + password with a verification link, Google, Facebook).
 *
 * A new or unverified password account is held in memory ([pending]) until its email is confirmed; only then does the
 * session get stored and the app see the user as signed in. The age is not part of a Firebase account, so it stays in
 * the stored profile on this phone.
 */
class FirebaseAuthRepository internal constructor(
    private val api: FirebaseAuthApi,
    private val sessions: SessionManagerImpl,
    private val tokens: FirebaseSession,
    private val social: SocialSignIn,
    /** Erases what NixUra's server keeps for this account (the Notion link). Best effort; runs once the password is confirmed. */
    private val eraseServerData: suspend () -> Unit = {},
) : AuthRepository {

    /** A signed-up or signed-in account whose email is not confirmed yet. Never persisted. */
    private data class Pending(val email: String, val reply: FbSignInReply, val name: String)

    private val pendingLock = Mutex()
    private var pending: Pending? = null

    override suspend fun register(email: String, password: String, name: String?): AuthResult<Unit> =
        when (val r = api.signUp(email, password)) {
            is FbResult.Ok -> {
                val display = name?.trim().orEmpty()
                val reply = if (display.isNotEmpty()) {
                    (api.update(r.value.idToken, displayName = display) as? FbResult.Ok)?.value?.withTokensFrom(r.value) ?: r.value
                } else {
                    r.value
                }
                pendingLock.withLock { pending = Pending(email, reply, display) }
                sendVerification(reply.idToken)
            }
            is FbResult.Err -> if (r.code == "EMAIL_EXISTS") AuthResult.Failure(AuthError(AuthErrorKind.EmailInUse)) else failure(r)
            FbResult.Offline -> failure(r)
        }

    override suspend fun checkEmailVerified(email: String): AuthResult<AuthUser> {
        val p = pendingFor(email) ?: return AuthResult.Failure(AuthError(AuthErrorKind.SessionExpired))
        val user = when (val r = api.lookup(p.reply.idToken)) {
            is FbResult.Ok -> r.value
            else -> return failure(r)
        }
        if (!user.emailVerified) return AuthResult.Failure(AuthError(AuthErrorKind.EmailNotVerified))
        // A fresh token carries email_verified = true, which the cloud database rules check.
        val renewed = when (val r = api.refresh(p.reply.refreshToken)) {
            is FbResult.Ok -> p.reply.copy(idToken = r.value.idToken, refreshToken = r.value.refreshToken, expiresIn = r.value.expiresIn)
            else -> p.reply
        }
        pendingLock.withLock { pending = null }
        return AuthResult.Success(startSession(renewed, user, p.name))
    }

    override suspend fun resendVerification(email: String): AuthResult<Unit> {
        val p = pendingFor(email) ?: return AuthResult.Failure(AuthError(AuthErrorKind.SessionExpired))
        return sendVerification(p.reply.idToken)
    }

    override suspend fun login(email: String, password: String): AuthResult<AuthUser> {
        val reply = when (val r = api.signInWithPassword(email, password)) {
            is FbResult.Ok -> r.value
            else -> return failure(r)
        }
        val user = when (val r = api.lookup(reply.idToken)) {
            is FbResult.Ok -> r.value
            else -> return failure(r)
        }
        if (!user.emailVerified) {
            pendingLock.withLock { pending = Pending(email, reply, user.displayName) }
            sendVerification(reply.idToken)
            return AuthResult.Failure(AuthError(AuthErrorKind.EmailNotVerified))
        }
        return AuthResult.Success(startSession(reply, user, user.displayName))
    }

    override suspend fun forgotPassword(email: String): AuthResult<Unit> = when (val r = api.sendPasswordReset(email)) {
        is FbResult.Ok -> AuthResult.Success(Unit)
        is FbResult.Err -> if (r.code == "EMAIL_NOT_FOUND") AuthResult.Success(Unit) else failure(r)
        FbResult.Offline -> failure(r)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult<Unit> {
        val session = sessions.current() ?: return AuthResult.Failure(AuthError(AuthErrorKind.SessionExpired))
        // Signing in again checks the current password and gives the fresh token Firebase wants for this change.
        val fresh = when (val r = api.signInWithPassword(session.user.email, currentPassword)) {
            is FbResult.Ok -> r.value
            else -> return failure(r)
        }
        return when (val r = api.update(fresh.idToken, password = newPassword)) {
            is FbResult.Ok -> {
                sessions.replaceTokens(session.sessionId, r.value.idToken, r.value.refreshToken, tokens.expiryOf(r.value.expiresIn))
                AuthResult.Success(Unit)
            }
            else -> failure(r)
        }
    }

    override fun isAvailable(provider: SocialProvider): Boolean = social.isAvailable(provider)

    override suspend fun signInWith(provider: SocialProvider): AuthResult<AuthUser> {
        val token = when (val t = social.token(provider)) {
            is SocialToken.Ok -> t.value
            SocialToken.Cancelled -> return AuthResult.Failure(AuthError(AuthErrorKind.Cancelled))
            SocialToken.NotAvailable -> return AuthResult.Failure(AuthError(AuthErrorKind.NotAvailable))
            SocialToken.Offline -> return AuthResult.Failure(AuthError(AuthErrorKind.Network))
            SocialToken.Failed -> return AuthResult.Failure(AuthError(AuthErrorKind.Unknown))
        }
        val postBody = when (provider) {
            SocialProvider.Google -> "id_token=${token.encodeURLParameter()}&providerId=google.com"
            SocialProvider.Facebook -> "access_token=${token.encodeURLParameter()}&providerId=facebook.com"
        }
        val reply = when (val r = api.signInWithIdp(postBody)) {
            is FbResult.Ok -> r.value
            else -> return failure(r)
        }
        if (reply.needConfirmation || reply.idToken.isEmpty()) return AuthResult.Failure(AuthError(AuthErrorKind.AccountExistsWithOtherMethod))
        val user = (api.lookup(reply.idToken) as? FbResult.Ok)?.value
            ?: FbUser(reply.localId, reply.email, reply.displayName, reply.emailVerified)
        return AuthResult.Success(startSession(reply, user, user.displayName.ifBlank { reply.displayName }))
    }

    override suspend fun deleteAccount(password: String?): AuthResult<Unit> {
        val session = sessions.current() ?: return AuthResult.Failure(AuthError(AuthErrorKind.SessionExpired))
        val idToken = if (password != null) {
            when (val r = api.signInWithPassword(session.user.email, password)) {
                is FbResult.Ok -> r.value.idToken
                else -> return failure(r)
            }
        } else {
            when (val t = tokens.idToken()) {
                is IdTokenResult.Ok -> t.idToken
                IdTokenResult.Ended -> return AuthResult.Failure(AuthError(AuthErrorKind.SessionExpired))
                IdTokenResult.Offline -> return AuthResult.Failure(AuthError(AuthErrorKind.Network))
            }
        }
        // The Worker cannot hear about a deleted Firebase account, so its Notion link is removed while the session still works.
        runCatching { eraseServerData() }
        return when (val r = api.delete(idToken)) {
            is FbResult.Ok -> {
                sessions.signOutLocally()
                AuthResult.Success(Unit)
            }
            else -> failure(r)
        }
    }

    /** Firebase tokens cannot be revoked from the phone; forgetting them ends the session here. */
    override suspend fun logout() {
        pendingLock.withLock { pending = null }
        sessions.signOutLocally()
    }

    override suspend fun fetchProfile(): AuthResult<AuthUser> {
        val stored = sessions.current()?.user ?: return AuthResult.Failure(AuthError(AuthErrorKind.SessionExpired))
        val idToken = when (val t = tokens.idToken()) {
            is IdTokenResult.Ok -> t.idToken
            IdTokenResult.Ended -> return AuthResult.Failure(AuthError(AuthErrorKind.SessionExpired))
            IdTokenResult.Offline -> return AuthResult.Failure(AuthError(AuthErrorKind.Network))
        }
        val remote = when (val r = api.lookup(idToken)) {
            is FbResult.Ok -> r.value
            else -> return failure(r)
        }
        val user = remote.toAuthUser(name = remote.displayName, age = stored.age)
        sessions.updateUser(user)
        return AuthResult.Success(user)
    }

    override suspend fun updateProfile(name: String?, age: Int?): AuthResult<AuthUser> {
        val stored = sessions.current()?.user ?: return AuthResult.Failure(AuthError(AuthErrorKind.SessionExpired))
        var newName = stored.name
        if (name != null) {
            val idToken = when (val t = tokens.idToken()) {
                is IdTokenResult.Ok -> t.idToken
                IdTokenResult.Ended -> return AuthResult.Failure(AuthError(AuthErrorKind.SessionExpired))
                IdTokenResult.Offline -> return AuthResult.Failure(AuthError(AuthErrorKind.Network))
            }
            when (val r = api.update(idToken, displayName = name)) {
                is FbResult.Ok -> newName = name
                else -> return failure(r)
            }
        }
        val user = AuthUser(stored.id, stored.email, newName, age, stored.emailVerified, stored.hasPassword)
        sessions.updateUser(user)
        return AuthResult.Success(user)
    }

    // ------------------------------------------------------------------ helpers

    private suspend fun pendingFor(email: String): Pending? = pendingLock.withLock { pending?.takeIf { it.email == email } }

    private suspend fun sendVerification(idToken: String): AuthResult<Unit> = when (val r = api.sendVerificationEmail(idToken)) {
        is FbResult.Ok -> AuthResult.Success(Unit)
        else -> failure(r)
    }

    private suspend fun startSession(reply: FbSignInReply, user: FbUser, name: String): AuthUser {
        val signedIn = user.toAuthUser(name = name, age = null)
        val stored = StoredUser(signedIn.id, signedIn.email, signedIn.name, null, signedIn.emailVerified, signedIn.hasPassword)
        sessions.signIn(StoredSession(reply.idToken, reply.refreshToken, signedIn.id, stored, tokens.expiryOf(reply.expiresIn)))
        return signedIn
    }

    private fun FbUser.toAuthUser(name: String, age: Int?) = AuthUser(
        id = localId,
        email = email,
        name = name,
        age = age,
        emailVerified = emailVerified,
        hasPassword = providerUserInfo.isEmpty() || providerUserInfo.any { it.providerId == "password" },
    )

    /** accounts:update answers without new tokens unless the password changed; keep the sign-up ones then. */
    private fun FbSignInReply.withTokensFrom(original: FbSignInReply) =
        if (idToken.isEmpty()) original.copy(displayName = displayName) else this

    private fun failure(result: FbResult<*>): AuthResult.Failure = AuthResult.Failure(FirebaseErrorMapper.map(result))
}
