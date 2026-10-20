package com.example.reclaimapp.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatformTools

fun initKoinIfNeeded(vararg modules: Module) {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin {
            modules(modules.toList())
        }
    }
}
