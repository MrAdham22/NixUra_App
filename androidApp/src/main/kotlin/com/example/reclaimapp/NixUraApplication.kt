package com.example.reclaimapp

import android.app.Application
import com.example.reclaimapp.di.initAndroidKoin
import com.example.reclaimapp.notifications.AndroidNotificationPermission
import com.example.reclaimapp.notifications.AndroidNotificationScheduler
import com.example.reclaimapp.notifications.ReminderReceiver

class NixUraApplication : Application() {

    val notificationPermission by lazy { AndroidNotificationPermission(this) }

    override fun onCreate() {
        super.onCreate()
        ReminderReceiver.ensureChannel(this)
        initAndroidKoin(this, AndroidNotificationScheduler(this), notificationPermission)
    }
}
