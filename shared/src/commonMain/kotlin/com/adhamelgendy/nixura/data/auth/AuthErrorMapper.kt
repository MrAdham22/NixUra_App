package com.adhamelgendy.nixura.data.auth

import com.adhamelgendy.nixura.domain.auth.AuthError
import com.adhamelgendy.nixura.domain.auth.AuthErrorKind

/** Turns an HTTP answer into a typed [AuthError]. Never exposes server text to the UI (strings are localised there). */
internal object AuthErrorMapper {

    fun codeOf(body: String): String? =
        runCatching { AuthJson.decodeFromString(ErrorEnvelope.serializer(), body).error?.code }.getOrNull()?.takeIf { it.isNotEmpty() }

    fun map(response: TransportResponse): AuthError {
        if (response.isNetworkFailure) return AuthError(AuthErrorKind.Network)
        val parsed = runCatching { AuthJson.decodeFromString(ErrorEnvelope.serializer(), response.body).error }.getOrNull()
        val retry = parsed?.retryAfterSeconds ?: response.retryAfterSeconds
        val kind = when (parsed?.code) {
            "invalid_credentials" -> AuthErrorKind.InvalidCredentials
            "email_not_verified" -> AuthErrorKind.EmailNotVerified
            "invalid_code" -> AuthErrorKind.InvalidCode
            "account_locked" -> AuthErrorKind.AccountLocked
            "rate_limited" -> AuthErrorKind.RateLimited
            "weak_password" -> AuthErrorKind.WeakPassword
            "invalid_input" -> AuthErrorKind.InvalidInput
            "unauthorized", "session_revoked" -> AuthErrorKind.SessionExpired
            else -> when {
                response.status == 429 -> AuthErrorKind.RateLimited
                response.status >= 500 -> AuthErrorKind.Server
                response.status == 422 -> AuthErrorKind.InvalidInput
                else -> AuthErrorKind.Unknown
            }
        }
        return AuthError(kind, retryAfterSeconds = retry, passwordProblems = if (kind == AuthErrorKind.WeakPassword) parsed?.problems.orEmpty() else emptyList())
    }
}
