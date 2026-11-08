package com.adhamelgendy.nixura.data.auth

import com.adhamelgendy.nixura.domain.auth.AuthErrorKind
import com.adhamelgendy.nixura.domain.auth.AuthResult
import com.adhamelgendy.nixura.domain.auth.SessionState
import com.adhamelgendy.nixura.domain.auth.SocialProvider
import com.adhamelgendy.nixura.domain.auth.SocialSignIn
import com.adhamelgendy.nixura.domain.auth.SocialToken
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirebaseAuthRepositoryTest {

    private val clock = FixedClock()

    private class FakeSocial(var token: SocialToken = SocialToken.Ok("g-token")) : SocialSignIn {
        override fun isAvailable(provider: SocialProvider) = provider == SocialProvider.Google
        override suspend fun token(provider: SocialProvider) = token
    }

    private fun signInReply(id: String = "u1", verified: Boolean = false) =
        TransportResponse(200, """{"idToken":"id-$id","refreshToken":"rt-$id","expiresIn":"3600","localId":"$id","email":"ann@example.com","emailVerified":$verified}""")

    private fun lookup(verified: Boolean, providers: String = """[{"providerId":"password"}]""", name: String = "Ann") =
        TransportResponse(200, """{"users":[{"localId":"u1","email":"ann@example.com","displayName":"$name","emailVerified":$verified,"providerUserInfo":$providers}]}""")

    private class Fixture(val repo: FirebaseAuthRepository, val sessions: SessionManagerImpl, val firebase: FakeFirebase)

    private fun fixture(social: SocialSignIn = FakeSocial(), handler: (String, String) -> TransportResponse): Fixture {
        val firebase = FakeFirebase(handler)
        val sessions = SessionManagerImpl(InMemorySecureStore())
        val api = FirebaseAuthApi(FirebaseConfig("k"), firebase)
        return Fixture(FirebaseAuthRepository(api, sessions, FirebaseSession(api, sessions, clock), social), sessions, firebase)
    }

    @Test fun registerMailsALinkAndSignsInOnlyOnceTheEmailIsConfirmed() = runTest {
        var verified = false
        val f = fixture { m, _ ->
            when (m) {
                "accounts:signUp" -> signInReply()
                "accounts:update" -> TransportResponse(200, """{"localId":"u1","displayName":"Ann"}""")
                "accounts:sendOobCode" -> TransportResponse(200, "{}")
                "accounts:lookup" -> lookup(verified)
                "token" -> TransportResponse(200, REFRESH_OK)
                else -> error(m)
            }
        }
        assertIs<AuthResult.Success<Unit>>(f.repo.register("ann@example.com", "a good password 1", "Ann"))
        assertEquals(1, f.firebase.count("accounts:sendOobCode"))
        assertTrue(f.firebase.calls.first { it.first == "accounts:sendOobCode" }.second.contains("VERIFY_EMAIL"))
        assertNull(f.sessions.current(), "not signed in before the link is tapped")

        val early = f.repo.checkEmailVerified("ann@example.com")
        assertEquals(AuthErrorKind.EmailNotVerified, (early as AuthResult.Failure).error.kind)

        verified = true
        val done = f.repo.checkEmailVerified("ann@example.com")
        assertEquals("u1", (done as AuthResult.Success).value.id)
        assertEquals("Ann", done.value.name)
        // The token renewed after verification carries email_verified, which the database rules check.
        assertEquals("new", f.sessions.current()?.accessToken)
        assertIs<SessionState.SignedIn>(f.sessions.state.value)
    }

    @Test fun registeringAnExistingEmailSaysSoAndSendsNothing() = runTest {
        val f = fixture { m, _ -> if (m == "accounts:signUp") fbError("EMAIL_EXISTS") else error(m) }
        val r = f.repo.register("ann@example.com", "a good password 1", null) as AuthResult.Failure
        assertEquals(AuthErrorKind.EmailInUse, r.error.kind)
        assertEquals(0, f.firebase.count("accounts:sendOobCode"))
    }

    @Test fun loginWithAnUnconfirmedEmailMailsANewLinkAndStaysSignedOut() = runTest {
        val f = fixture { m, _ ->
            when (m) {
                "accounts:signInWithPassword" -> signInReply()
                "accounts:lookup" -> lookup(verified = false)
                "accounts:sendOobCode" -> TransportResponse(200, "{}")
                else -> error(m)
            }
        }
        val r = f.repo.login("ann@example.com", "pw")
        assertEquals(AuthErrorKind.EmailNotVerified, (r as AuthResult.Failure).error.kind)
        assertEquals(1, f.firebase.count("accounts:sendOobCode"))
        assertNull(f.sessions.current())
    }

    @Test fun loginWithAConfirmedEmailStoresTheSession() = runTest {
        val f = fixture { m, _ ->
            when (m) {
                "accounts:signInWithPassword" -> signInReply()
                "accounts:lookup" -> lookup(verified = true)
                else -> error(m)
            }
        }
        val r = f.repo.login("ann@example.com", "pw") as AuthResult.Success
        assertTrue(r.value.hasPassword)
        assertEquals("id-u1", f.sessions.current()?.accessToken)
        assertEquals(clock.now + 3_600_000, f.sessions.current()?.expiresAt)
    }

    @Test fun wrongPasswordIsInvalidCredentials() = runTest {
        val f = fixture { _, _ -> fbError("INVALID_LOGIN_CREDENTIALS") }
        assertEquals(AuthErrorKind.InvalidCredentials, (f.repo.login("ann@example.com", "x") as AuthResult.Failure).error.kind)
    }

    @Test fun offlineIsANetworkError() = runTest {
        val f = fixture { _, _ -> TransportResponse(0) }
        assertEquals(AuthErrorKind.Network, (f.repo.login("ann@example.com", "x") as AuthResult.Failure).error.kind)
    }

    @Test fun forgotPasswordAnswersTheSameForUnknownEmails() = runTest {
        val f = fixture { _, _ -> fbError("EMAIL_NOT_FOUND") }
        assertIs<AuthResult.Success<Unit>>(f.repo.forgotPassword("nobody@example.com"))
    }

    @Test fun googleSignInExchangesTheIdTokenAndHasNoPassword() = runTest {
        val f = fixture { m, body ->
            when (m) {
                "accounts:signInWithIdp" -> {
                    assertTrue(body.contains("providerId=google.com"))
                    signInReply(verified = true)
                }
                "accounts:lookup" -> lookup(verified = true, providers = """[{"providerId":"google.com"}]""")
                else -> error(m)
            }
        }
        val r = f.repo.signInWith(SocialProvider.Google) as AuthResult.Success
        assertFalse(r.value.hasPassword)
        assertIs<SessionState.SignedIn>(f.sessions.state.value)
    }

    @Test fun closingTheSheetIsCancelledNotAnError() = runTest {
        val f = fixture(FakeSocial(SocialToken.Cancelled)) { m, _ -> error(m) }
        assertEquals(AuthErrorKind.Cancelled, (f.repo.signInWith(SocialProvider.Google) as AuthResult.Failure).error.kind)
    }

    @Test fun anEmailThatSignsInAnotherWayIsReported() = runTest {
        val f = fixture { m, _ -> if (m == "accounts:signInWithIdp") TransportResponse(200, """{"needConfirmation":true,"email":"ann@example.com"}""") else error(m) }
        val r = f.repo.signInWith(SocialProvider.Google) as AuthResult.Failure
        assertEquals(AuthErrorKind.AccountExistsWithOtherMethod, r.error.kind)
        assertNull(f.sessions.current())
    }

    @Test fun deleteChecksThePasswordThenSignsOut() = runTest {
        val f = fixture { m, _ ->
            when (m) {
                "accounts:signInWithPassword" -> signInReply()
                "accounts:delete" -> TransportResponse(200, "{}")
                else -> error(m)
            }
        }
        f.sessions.signIn(storedSession(expiresAt = clock.now + 3_600_000))
        assertIs<AuthResult.Success<Unit>>(f.repo.deleteAccount("pw"))
        assertNull(f.sessions.current())
    }

    @Test fun deleteWithAnOldSignInAsksToSignInAgain() = runTest {
        val f = fixture { m, _ -> if (m == "accounts:delete") fbError("CREDENTIAL_TOO_OLD_LOGIN_AGAIN") else error(m) }
        f.sessions.signIn(storedSession(expiresAt = clock.now + 3_600_000))
        val r = f.repo.deleteAccount(null) as AuthResult.Failure
        assertEquals(AuthErrorKind.RecentLoginRequired, r.error.kind)
        assertIs<StoredSession>(f.sessions.current())
    }

    @Test fun changePasswordStoresTheNewTokens() = runTest {
        val f = fixture { m, _ ->
            when (m) {
                "accounts:signInWithPassword" -> signInReply()
                "accounts:update" -> TransportResponse(200, """{"idToken":"after","refreshToken":"rt-after","expiresIn":"3600","localId":"u1"}""")
                else -> error(m)
            }
        }
        f.sessions.signIn(storedSession(expiresAt = clock.now + 3_600_000))
        assertIs<AuthResult.Success<Unit>>(f.repo.changePassword("old pw", "a new good password 2"))
        assertEquals("after", f.sessions.current()?.accessToken)
        assertEquals("rt-after", f.sessions.current()?.refreshToken)
    }

    @Test fun parallelCallersShareOneTokenRefresh() = runTest {
        val f = fixture { m, _ -> if (m == "token") TransportResponse(200, REFRESH_OK) else error(m) }
        f.sessions.signIn(storedSession(expiresAt = 0))
        val api = FirebaseAuthApi(FirebaseConfig("k"), f.firebase)
        val tokens = FirebaseSession(api, f.sessions, clock)
        val results = List(5) { async { tokens.idToken() } }.awaitAll()
        assertTrue(results.all { it == IdTokenResult.Ok("new") })
        assertEquals(1, f.firebase.count("token"))
    }

    @Test fun requestsCarryTheFieldsFirebaseRequires() = runTest {
        val f = fixture { m, _ -> if (m == "accounts:signInWithIdp") signInReply(verified = true) else lookup(verified = true) }
        f.repo.signInWith(SocialProvider.Google)
        val idp = f.firebase.calls.first { it.first == "accounts:signInWithIdp" }.second
        assertTrue(idp.contains("\"requestUri\":\"http://localhost\""), idp)
        assertTrue(idp.contains("\"returnSecureToken\":true"), idp)
        f.repo.login("ann@example.com", "pw")
        assertTrue(f.firebase.calls.first { it.first == "accounts:signInWithPassword" }.second.contains("\"returnSecureToken\":true"))
    }

    @Test fun firebaseErrorCodesMapToKinds() {
        assertEquals(AuthErrorKind.WeakPassword, FirebaseErrorMapper.map("WEAK_PASSWORD").kind)
        assertEquals(AuthErrorKind.RateLimited, FirebaseErrorMapper.map("TOO_MANY_ATTEMPTS_TRY_LATER").kind)
        assertEquals(AuthErrorKind.AccountLocked, FirebaseErrorMapper.map("USER_DISABLED").kind)
        assertEquals(AuthErrorKind.NotAvailable, FirebaseErrorMapper.map("OPERATION_NOT_ALLOWED").kind)
        assertEquals(AuthErrorKind.Server, FirebaseErrorMapper.map("HTTP_503").kind)
        assertEquals(AuthErrorKind.Unknown, FirebaseErrorMapper.map("SOMETHING_NEW").kind)
    }
}
