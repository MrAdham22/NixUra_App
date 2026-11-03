package com.example.reclaimapp.di

import com.example.reclaimapp.domain.recognition.HabitRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatform
import org.koin.mp.KoinPlatformTools

fun initKoinIfNeeded(vararg modules: Module) {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin {
            modules(modules.toList())
        }
        warmUpInBackground()
    }
}

/**
 * Builds the habit catalog and its name index on a background thread right after start. Cards, the coach and water
 * detection all need it; built lazily on the main thread it froze the first frames.
 */
fun warmUpInBackground() {
    CoroutineScope(Dispatchers.Default).launch {
        runCatching { KoinPlatform.getKoin().get<HabitRecognizer>().recognize("smoking") }
    }
}
