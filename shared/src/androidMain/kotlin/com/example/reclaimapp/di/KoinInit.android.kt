package com.example.reclaimapp.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools

fun initAndroidKoin(context: Context) {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin {
            androidContext(context.applicationContext)
            modules(platformModule(context), appModule())
        }
    }
}
