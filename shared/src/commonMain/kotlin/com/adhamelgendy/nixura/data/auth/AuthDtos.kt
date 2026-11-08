@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.adhamelgendy.nixura.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Shared JSON settings: tolerant of new server fields, omits null/default request fields. */
internal val AuthJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = false
}

/** For Firebase requests: fields such as `returnSecureToken` and `requestUri` must be sent even at their default. */
internal val FirebaseRequestJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

// ---------------------------------------------------------------- Firebase Auth REST (Identity Toolkit v1)

@Serializable internal data class FbPasswordRequest(val email: String, val password: String, val returnSecureToken: Boolean = true)

@Serializable internal data class FbIdpRequest(
    val postBody: String,
    val requestUri: String = "http://localhost",
    val returnSecureToken: Boolean = true,
    val returnIdpCredential: Boolean = true,
)

@Serializable internal data class FbOobRequest(val requestType: String, val email: String? = null, val idToken: String? = null)
@Serializable internal data class FbIdTokenRequest(val idToken: String)

@Serializable internal data class FbUpdateRequest(
    val idToken: String,
    val displayName: String? = null,
    val password: String? = null,
    val returnSecureToken: Boolean = true,
)

/** signUp, signInWithPassword, signInWithIdp and update all answer with (a subset of) these fields. */
@Serializable internal data class FbSignInReply(
    val idToken: String = "",
    val refreshToken: String = "",
    val expiresIn: String = "3600",
    val localId: String = "",
    val email: String = "",
    val displayName: String = "",
    val emailVerified: Boolean = false,
    /** signInWithIdp: the email already belongs to an account that signs in another way. */
    val needConfirmation: Boolean = false,
    val errorMessage: String? = null,
)

@Serializable internal data class FbLookupReply(val users: List<FbUser> = emptyList())

@Serializable internal data class FbUser(
    val localId: String,
    val email: String = "",
    val displayName: String = "",
    val emailVerified: Boolean = false,
    val providerUserInfo: List<FbProviderInfo> = emptyList(),
)

@Serializable internal data class FbProviderInfo(val providerId: String = "")

@Serializable internal data class FbRefreshReply(
    @SerialName("id_token") val idToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: String = "3600",
    @SerialName("user_id") val userId: String = "",
)

@Serializable internal data class FbErrorEnvelope(val error: FbErrorBody? = null)
@Serializable internal data class FbErrorBody(val code: Int = 0, val message: String = "")

// ---------------------------------------------------------------- stored on the device

/** What is persisted (encrypted) on the device. Contains tokens, so it must only ever go through [SecureStore]. */
@Serializable internal data class StoredSession(
    /** Firebase ID token (valid for about an hour). */
    val accessToken: String,
    val refreshToken: String,
    /** The Firebase user id; tokens are only swapped while it still matches. */
    val sessionId: String,
    val user: StoredUser,
    /** When [accessToken] expires (epoch millis). 0 = unknown, refresh before the next call. */
    val expiresAt: Long = 0,
)

@Serializable internal data class StoredUser(
    val id: String,
    val email: String,
    val name: String = "",
    val age: Int? = null,
    val emailVerified: Boolean = true,
    val hasPassword: Boolean = true,
)
