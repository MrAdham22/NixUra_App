package com.adhamelgendy.nixura

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
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
import com.adhamelgendy.nixura.notifications.NotificationDeepLink
import com.adhamelgendy.nixura.shortcuts.ShortcutDeepLink
import com.adhamelgendy.nixura.widgets.WidgetDeepLink
import com.adhamelgendy.nixura.widgets.WidgetUpdater
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.adhamelgendy.nixura.domain.usecase.ObserveAnyNameHiddenUseCase

class MainActivity : ComponentActivity() {

    private val microphonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* the voice sheet re-checks the permission itself */ }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result handled by next isGranted() check */ }

    // Prayer-time location (phase C): the callback waiting for the system dialog's answer.
    private var locationResult: ((Boolean) -> Unit)? = null
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            locationResult?.invoke(granted)
            locationResult = null
        }

    private val appNavigator: Navigator by inject()
    private val anyNameHidden: ObserveAnyNameHiddenUseCase by inject()
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
        (application as NixUraApplication).ensureFacebookSdk()

        // Opened from a home-screen widget? Only on a fresh start: after rotation / process restore the
        // navigator already holds (or deliberately reset) the stack and the extras were consumed.
        // The same for launcher App Shortcuts (long-press the icon). Static shortcuts start the activity with
        // NEW_TASK | CLEAR_TASK, so even when the app was already running they arrive here, not in onNewIntent.
        if (savedInstanceState == null) {
            WidgetDeepLink.apply(intent, appNavigator)
            ShortcutDeepLink.apply(this, intent, appNavigator)
            NotificationDeepLink.apply(intent, appNavigator)
        }

        googleAuthorizer.consent = { pending ->
            suspendCancellableCoroutine { cont ->
                consentResult = { data -> if (cont.isActive) cont.resume(data) }
                googleConsentLauncher.launch(IntentSenderRequest.Builder(pending).build())
            }
        }

        socialSignIn.activity = this

        (application as NixUraApplication).voiceRecorder.launcher = {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        (application as NixUraApplication).deviceLocation.launcher = { onResult ->
            locationResult = onResult
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        (application as NixUraApplication).deviceLocation.rationale = {
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        (application as NixUraApplication).notificationPermission.launcher = {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        (application as NixUraApplication).notificationPermission.rationale = {
            Build.VERSION.SDK_INT >= 33 && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        }

        keepHiddenNamesOutOfRecents()

        setContent {
            val navigator = koinInject<Navigator>()
            BackHandler(enabled = navigator.canGoBack) { navigator.back() }
            App()
        }
    }

    /**
     * The recent-apps preview is a picture of the open screen, names included, so it is off while any quit habit's
     * name is hidden. Off until the first read too. Android 13+ only: before, FLAG_SECURE would also block screenshots.
     */
    private fun keepHiddenNamesOutOfRecents() {
        if (Build.VERSION.SDK_INT < 33) return
        setRecentsScreenshotEnabled(false)
        lifecycleScope.launch { anyNameHidden().collect { hidden -> setRecentsScreenshotEnabled(!hidden) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        WidgetDeepLink.apply(intent, appNavigator)
        ShortcutDeepLink.apply(this, intent, appNavigator) // some launchers deliver shortcuts to the running activity
        NotificationDeepLink.apply(intent, appNavigator) // a tap while the app is open
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
