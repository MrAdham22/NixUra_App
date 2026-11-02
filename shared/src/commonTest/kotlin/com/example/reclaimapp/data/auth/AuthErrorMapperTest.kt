package com.example.reclaimapp.data.auth

import com.example.reclaimapp.domain.auth.AuthErrorKind
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthErrorMapperTest {
    private fun err(code: String, extra: String = "") = """{"error":{"code":"$code","message":"m"$extra}}"""

    @Test fun mapsServerCodes() {
        val cases = mapOf(
            "invalid_credentials" to AuthErrorKind.InvalidCredentials,
            "email_not_verified" to AuthErrorKind.EmailNotVerified,
            "invalid_code" to AuthErrorKind.InvalidCode,
            "account_locked" to AuthErrorKind.AccountLocked,
            "rate_limited" to AuthErrorKind.RateLimited,
            "weak_password" to AuthErrorKind.WeakPassword,
            "invalid_input" to AuthErrorKind.InvalidInput,
            "unauthorized" to AuthErrorKind.SessionExpired,
            "session_revoked" to AuthErrorKind.SessionExpired,
        )
        for ((code, kind) in cases) assertEquals(kind, AuthErrorMapper.map(TransportResponse(400, err(code))).kind, code)
    }

    @Test fun networkFailureAndStatusFallbacks() {
        assertEquals(AuthErrorKind.Network, AuthErrorMapper.map(TransportResponse(0)).kind)
        assertEquals(AuthErrorKind.Server, AuthErrorMapper.map(TransportResponse(503, "<html>")).kind)
        assertEquals(AuthErrorKind.RateLimited, AuthErrorMapper.map(TransportResponse(429, "")).kind)
        assertEquals(AuthErrorKind.Unknown, AuthErrorMapper.map(TransportResponse(418, "teapot")).kind)
    }

    @Test fun retryAfterAndPasswordProblemsAreCarried() {
        val locked = AuthErrorMapper.map(TransportResponse(429, err("account_locked", ""","retryAfterSeconds":42""")))
        assertEquals(42, locked.retryAfterSeconds)
        assertEquals(17, AuthErrorMapper.map(TransportResponse(429, err("rate_limited"), retryAfterSeconds = 17)).retryAfterSeconds)
        val weak = AuthErrorMapper.map(TransportResponse(422, err("weak_password", ""","problems":["too_common","too_short"]""")))
        assertEquals(listOf("too_common", "too_short"), weak.passwordProblems)
    }
}
