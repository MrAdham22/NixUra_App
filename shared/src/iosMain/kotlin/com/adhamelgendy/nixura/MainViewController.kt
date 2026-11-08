package com.adhamelgendy.nixura

import androidx.compose.ui.window.ComposeUIViewController
import com.adhamelgendy.nixura.di.appModule
import com.adhamelgendy.nixura.di.authModule
import com.adhamelgendy.nixura.di.initKoinIfNeeded
import com.adhamelgendy.nixura.di.platformModule

fun MainViewController() = ComposeUIViewController {
    initKoinIfNeeded(platformModule(), appModule(), authModule())
    App()
}
