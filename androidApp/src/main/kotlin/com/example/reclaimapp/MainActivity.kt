package com.example.reclaimapp

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.widgets.WidgetDeepLink
import com.example.reclaimapp.widgets.WidgetUpdater
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result handled by next isGranted() check */ }

    private val appNavigator: Navigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Opened from a home-screen widget? Only on a fresh start: after rotation / process restore the
        // navigator already holds (or deliberately reset) the stack and the extras were consumed.
        if (savedInstanceState == null) WidgetDeepLink.apply(intent, appNavigator)

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

    override fun onStart() {
        super.onStart()
        // Cheap resync so widgets match what the user just saw in the app.
        WidgetUpdater.requestRefresh(applicationContext)
    }
}
