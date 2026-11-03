package com.example.reclaimapp.integrations

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.reclaimapp.domain.integration.GoogleAuthResult
import com.example.reclaimapp.domain.integration.GoogleAuthorizer
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
 * Google Calendar access through Google's Authorization API. Only the read-only calendar scope is asked for. The
 * access token lives for about an hour and is fetched again for every sync (silently once the user agreed), so nothing
 * secret is stored on the phone. No client id is in the code: Google matches the app by package name and signing
 * certificate (see README, "Google Calendar setup"); without that setup Google answers DEVELOPER_ERROR → NotAvailable.
 *
 * [consent] shows Google's consent screen; MainActivity sets it while it is alive.
 */
class AndroidGoogleAuthorizer(private val context: Context) : GoogleAuthorizer {

    /** Launches Google's consent screen and returns its result intent, or null when the user backed out. */
    @Volatile
    var consent: (suspend (PendingIntent) -> Intent?)? = null

    private val request: AuthorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(CALENDAR_READONLY))).build()

    override suspend fun authorize(interactive: Boolean): GoogleAuthResult {
        val client = Identity.getAuthorizationClient(context)
        val first = try {
            client.authorize(request).awaitResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return failure(e)
        }
        val result = if (first.hasResolution()) {
            if (!interactive) return GoogleAuthResult.NeedsConsent
            val show = consent ?: return GoogleAuthResult.NeedsConsent
            val pending = first.pendingIntent ?: return GoogleAuthResult.Denied
            val data = show(pending) ?: return GoogleAuthResult.Denied
            try {
                client.getAuthorizationResultFromIntent(data)
            } catch (e: Exception) {
                return GoogleAuthResult.Denied
            }
        } else {
            first
        }
        val token = result.accessToken ?: return GoogleAuthResult.Denied
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
    }

    private fun failure(e: Exception): GoogleAuthResult = when ((e as? ApiException)?.statusCode) {
        CommonStatusCodes.NETWORK_ERROR -> GoogleAuthResult.Offline
        CommonStatusCodes.DEVELOPER_ERROR, CommonStatusCodes.API_NOT_CONNECTED -> GoogleAuthResult.NotAvailable
        else -> GoogleAuthResult.Denied
    }

    private suspend fun Task<AuthorizationResult>.awaitResult(): AuthorizationResult = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWith(Result.failure(it)) }
    }

    private companion object {
        const val CALENDAR_READONLY = "https://www.googleapis.com/auth/calendar.readonly"
    }
}
