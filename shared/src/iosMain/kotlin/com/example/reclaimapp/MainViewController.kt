package com.example.reclaimapp

import androidx.compose.ui.window.ComposeUIViewController
import com.example.reclaimapp.di.appModule
import com.example.reclaimapp.di.initKoinIfNeeded
import com.example.reclaimapp.di.platformModule

fun MainViewController() = ComposeUIViewController {
    initKoinIfNeeded(platformModule(), appModule())
    App()
}
