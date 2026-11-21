package com.adhamelgendy.nixura

import androidx.compose.ui.window.ComposeUIViewController
import com.adhamelgendy.nixura.di.appModule
import com.adhamelgendy.nixura.di.authModule
import com.adhamelgendy.nixura.di.initKoinIfNeeded
import com.adhamelgendy.nixura.di.platformModule

fun MainViewController() = ComposeUIViewController {
    initKoinIfNeeded(platformModule(), appModule(), authModule())
    // A quick action that cold-started the app arrived before Koin existed (IosShortcuts).
    androidx.compose.runtime.remember { IosShortcuts.applyPending(org.koin.mp.KoinPlatform.getKoin().get()); true }
    App()
}
