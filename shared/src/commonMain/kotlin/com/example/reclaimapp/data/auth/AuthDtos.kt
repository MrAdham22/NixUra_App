@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.example.reclaimapp.data.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Shared JSON settings: tolerant of new server fields, omits null/default request fields. */
internal val AuthJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = false
}

@Serializable internal data class RegisterRequest(val email: String, val password: String, val name: String? = null)
@Serializable internal data class VerifyRequest(val email: String, val code: String, val deviceName: String, val platform: String)
@Serializable internal data class EmailRequest(val email: String)
@Serializable internal data class LoginRequest(val email: String, val password: String, val deviceName: String, val platform: String)
@Serializable internal data class RefreshRequest(val refreshToken: String)
@Serializable internal data class ResetRequest(val email: String, val code: String, val newPassword: String)
@Serializable internal data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
@Serializable internal data class PasswordRequest(val password: String)

@Serializable internal data class TokensDto(
    val accessToken: String,
    val accessTokenExpiresInSeconds: Int = 900,
    val refreshToken: String,
    val sessionId: String,
)

@Serializable internal data class UserDto(
    val id: String,
    val email: String,
    val name: String = "",
    val age: Int? = null,
    val emailVerified: Boolean = false,
)

@Serializable internal data class AuthResultDto(val tokens: TokensDto, val user: UserDto)
@Serializable internal data class UserEnvelope(val user: UserDto)

@Serializable internal data class SessionDto(
    val id: String,
    val deviceName: String = "",
    val platform: String = "",
    val lastUsedAt: String = "",
    val current: Boolean = false,
)

@Serializable internal data class SessionsEnvelope(val sessions: List<SessionDto> = emptyList())

@Serializable internal data class ErrorEnvelope(val error: ErrorBody? = null)

@Serializable internal data class ErrorBody(
    val code: String = "",
    val message: String = "",
    val retryAfterSeconds: Int? = null,
    val problems: List<String> = emptyList(),
)

/** What is persisted (encrypted) on the device. Contains tokens, so it must only ever go through [SecureStore]. */
@Serializable internal data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val sessionId: String,
    val user: StoredUser,
)

@Serializable internal data class StoredUser(
    val id: String,
    val email: String,
    val name: String = "",
    val age: Int? = null,
    val emailVerified: Boolean = true,
)
