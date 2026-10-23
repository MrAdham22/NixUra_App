package com.example.reclaimapp

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.reclaimapp.presentation.navigation.Navigator
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result handled by next isGranted() check */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        (application as NixUraApplication).notificationPermission.launcher = {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val navigator = koinInject<Navigator>()
            BackHandler(enabled = navigator.canGoBack) { navigator.back() }
            App()
        }
    }
}
