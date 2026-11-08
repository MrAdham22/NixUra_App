package com.adhamelgendy.nixura.integrations

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResult
import com.adhamelgendy.nixura.domain.integration.GoogleAuthResult
import com.adhamelgendy.nixura.domain.integration.GoogleAuthorizer
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume

/**
 * Google Calendar and Google Tasks access through Google's Authorization API. Only read-only scopes are asked for. The
 * access token lives for about an hour and is fetched again for every sync (silently once the user agreed), so nothing
 * secret is stored on the phone. No client id is in the code: Google matches the app by package name and signing
 * certificate (see README, "Google Calendar setup"); without that setup Google answers DEVELOPER_ERROR → NotAvailable.
 *
 * [consent] shows Google's consent screen; MainActivity sets it while it is alive. Failures are logged under [TAG]
 * with Google's status code only (never the token or the account).
 */
class AndroidGoogleAuthorizer(private val context: Context) : GoogleAuthorizer {

    /** Launches Google's consent screen and returns how it closed (result code and data). */
    @Volatile
    var consent: (suspend (PendingIntent) -> ActivityResult)? = null

    private val request: AuthorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(CALENDAR_READONLY), Scope(TASKS_READONLY))).build()

    override suspend fun authorize(interactive: Boolean): GoogleAuthResult {
        val client = Identity.getAuthorizationClient(context)
        val first = try {
            client.authorize(request).awaitResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return failure("authorize", e)
        }
        val result = if (first.hasResolution()) {
            if (!interactive) return GoogleAuthResult.NeedsConsent
            val show = consent ?: run {
                Log.w(TAG, "consent needed but no screen is open to show it")
                return GoogleAuthResult.NeedsConsent
            }
            val pending = first.pendingIntent ?: run {
                Log.w(TAG, "consent needed but Google gave no pending intent")
                return GoogleAuthResult.Denied
            }
            val closed = show(pending)
            val data = closed.data
            if (data == null) {
                Log.w(TAG, "consent screen closed with result code ${closed.resultCode} and no data")
                return if (closed.resultCode == Activity.RESULT_CANCELED) GoogleAuthResult.Cancelled else GoogleAuthResult.Denied
            }
            // Also read the data when the screen did not return RESULT_OK: Google puts the failure status in it.
            try {
                client.getAuthorizationResultFromIntent(data)
            } catch (e: Exception) {
                return failure("consent (result code ${closed.resultCode})", e)
            }
        } else {
            first
        }
        val token = result.accessToken ?: run {
            Log.w(TAG, "authorization finished without an access token")
            return GoogleAuthResult.Denied
        }
        return GoogleAuthResult.Granted(token, result.toGoogleSignInAccount()?.email.orEmpty())
    }

    override suspend fun revoke() {
        val token = (authorize(interactive = false) as? GoogleAuthResult.Granted)?.accessToken ?: return
        withContext(Dispatchers.IO) {
            runCatching {
                val c = URL("https://oauth2.googleapis.com/revoke").openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.doOutput = true
                c.connectTimeout = 10_000
                c.readTimeout = 10_000
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                c.outputStream.use { it.write("token=${URLEncoder.encode(token, "UTF-8")}".toByteArray()) }
                c.responseCode
                c.disconnect()
            }
        }
        // Google's services keep handing out the revoked token otherwise, and the next Connect gets 401s.
        forget(token)
    }

    override suspend fun forget(accessToken: String) {
        withContext(Dispatchers.IO) {
            runCatching { GoogleAuthUtil.clearToken(context, accessToken) }
                .onFailure { Log.w(TAG, "clearing a rejected token failed: ${it.javaClass.simpleName}") }
        }
    }

    private fun failure(step: String, e: Exception): GoogleAuthResult {
        val status = (e as? ApiException)?.statusCode
        val name = status?.let { CommonStatusCodes.getStatusCodeString(it) } ?: e.javaClass.simpleName
        Log.w(TAG, "$step failed: status=$status ($name) message=${e.message}")
        // Google answers INTERNAL_ERROR (8) when no Android OAuth client matches the package + SHA-1; only the message names it.
        if (e.message?.contains(UNREGISTERED) == true) return GoogleAuthResult.NotAvailable
        return when (status) {
            CommonStatusCodes.NETWORK_ERROR -> GoogleAuthResult.Offline
            CommonStatusCodes.DEVELOPER_ERROR, CommonStatusCodes.API_NOT_CONNECTED -> GoogleAuthResult.NotAvailable
            CommonStatusCodes.CANCELED -> GoogleAuthResult.Cancelled
            else -> GoogleAuthResult.Denied
        }
    }

    private suspend fun Task<AuthorizationResult>.awaitResult(): AuthorizationResult = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWith(Result.failure(it)) }
    }

    private companion object {
        const val TAG = "NixUraGoogleAuth"
        const val UNREGISTERED = "UNREGISTERED_ON_API_CONSOLE"
        const val CALENDAR_READONLY = "https://www.googleapis.com/auth/calendar.readonly"
        const val TASKS_READONLY = "https://www.googleapis.com/auth/tasks.readonly"
    }
}
