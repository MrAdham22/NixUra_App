package com.adhamelgendy.nixura

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.adhamelgendy.nixura.auth.AndroidSocialSignIn
import com.adhamelgendy.nixura.integrations.AndroidGoogleAuthorizer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.widgets.WidgetDeepLink
import com.adhamelgendy.nixura.widgets.WidgetUpdater
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result handled by next isGranted() check */ }

    private val appNavigator: Navigator by inject()
    private val googleAuthorizer: AndroidGoogleAuthorizer by inject()
    private val socialSignIn: AndroidSocialSignIn by inject()

    // Google's consent screen for calendar access; the authorizer suspends until it closes.
    private var consentResult: ((ActivityResult) -> Unit)? = null
    private val googleConsentLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
            consentResult?.invoke(r)
            consentResult = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Start with light bar icons, whatever the system theme: the app opens dark. SystemBarsEffect follows the app's theme after that.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // Opened from a home-screen widget? Only on a fresh start: after rotation / process restore the
        // navigator already holds (or deliberately reset) the stack and the extras were consumed.
        if (savedInstanceState == null) WidgetDeepLink.apply(intent, appNavigator)

        googleAuthorizer.consent = { pending ->
            suspendCancellableCoroutine { cont ->
                consentResult = { data -> if (cont.isActive) cont.resume(data) }
                googleConsentLauncher.launch(IntentSenderRequest.Builder(pending).build())
            }
        }

        socialSignIn.activity = this

        (application as NixUraApplication).notificationPermission.launcher = {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val navigator = koinInject<Navigator>()
            BackHandler(enabled = navigator.canGoBack) { navigator.back() }
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        WidgetDeepLink.apply(intent, appNavigator)
    }

    override fun onDestroy() {
        googleAuthorizer.consent = null
        if (socialSignIn.activity === this) socialSignIn.activity = null
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        // Cheap resync so widgets match what the user just saw in the app.
        WidgetUpdater.requestRefresh(applicationContext)
    }
}
