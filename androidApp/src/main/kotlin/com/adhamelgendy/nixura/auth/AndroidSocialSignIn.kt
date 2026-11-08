package com.adhamelgendy.nixura.auth

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.adhamelgendy.nixura.domain.auth.SocialProvider
import com.adhamelgendy.nixura.domain.auth.SocialSignIn
import com.adhamelgendy.nixura.domain.auth.SocialToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google (Credential Manager, "Sign in with Google") and Facebook (Facebook Login) sheets on Android.
 * [activity] is set by MainActivity while it is alive: both sheets need it. Never logs tokens or emails.
 *
 * @param webClientId the Firebase project's web OAuth client (`default_web_client_id`), which Google puts in the ID token.
 */
class AndroidSocialSignIn(
    private val webClientId: String,
    private val facebookConfigured: Boolean,
) : SocialSignIn {

    var activity: ComponentActivity? = null

    override fun isAvailable(provider: SocialProvider): Boolean = when (provider) {
        SocialProvider.Google -> webClientId.isNotBlank()
        SocialProvider.Facebook -> facebookConfigured && FacebookSdk.isInitialized()
    }

    override suspend fun token(provider: SocialProvider): SocialToken {
        if (!isAvailable(provider)) return SocialToken.NotAvailable
        val host = activity ?: return SocialToken.Failed
        return when (provider) {
            SocialProvider.Google -> google(host)
            SocialProvider.Facebook -> facebook(host)
        }
    }

    private suspend fun google(host: ComponentActivity): SocialToken {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
        return try {
            val credential = CredentialManager.create(host).getCredential(host, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                SocialToken.Ok(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Log.w(TAG, "google: unexpected credential type ${credential.type}")
                SocialToken.Failed
            }
        } catch (e: GetCredentialCancellationException) {
            SocialToken.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "google: no account available (${e.type})")
            SocialToken.Failed
        } catch (e: GetCredentialException) {
            Log.w(TAG, "google: ${e.type} ${e.message}")
            SocialToken.Failed
        }
    }

    private suspend fun facebook(host: ComponentActivity): SocialToken {
        val manager = LoginManager.getInstance()
        val callbacks = CallbackManager.Factory.create()
        return try {
            suspendCancellableCoroutine { cont ->
                manager.registerCallback(callbacks, object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        if (cont.isActive) cont.resume(SocialToken.Ok(result.accessToken.token))
                    }

                    override fun onCancel() {
                        if (cont.isActive) cont.resume(SocialToken.Cancelled)
                    }

                    override fun onError(error: FacebookException) {
                        Log.w(TAG, "facebook: ${error.javaClass.simpleName} ${error.message}")
                        if (cont.isActive) cont.resume(SocialToken.Failed)
                    }
                })
                manager.logIn(host, callbacks, listOf("email", "public_profile"))
            }
        } finally {
            manager.unregisterCallback(callbacks)
        }
    }

    private companion object {
        const val TAG = "NixUraSignIn"
    }
}
