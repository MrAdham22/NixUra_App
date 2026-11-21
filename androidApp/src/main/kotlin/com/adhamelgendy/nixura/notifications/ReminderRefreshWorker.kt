package com.adhamelgendy.nixura.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.adhamelgendy.nixura.domain.notification.NotificationSyncService
import org.koin.core.context.GlobalContext
import java.util.concurrent.TimeUnit

/**
 * Safety net for good-habit reminders (phase C). The plan covers today and tomorrow and is normally extended by every
 * delivered reminder, every log (app, widget, notification button), app start, boot and clock change. On a day with
 * none of those (all goals met, app not opened) this job re-plans every 12 hours, so prayer times roll over to the
 * next day before the two-day plan runs out.
 *
 * WorkManager, not an alarm: the moment does not matter (anywhere in the day is fine), it needs no network, survives
 * reboots by itself and lets the OS batch it with other work (cheap on battery, tolerated by ColorOS).
 */
class ReminderRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val service = GlobalContext.getOrNull()?.getOrNull<NotificationSyncService>() ?: return Result.retry()
        service.syncOnce()
        return Result.success()
    }

    companion object {
        private const val NAME = "nixura-reminder-refresh"

        /** Idempotent: keeps the existing schedule. Called on every app start. */
        fun schedule(context: Context) {
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderRefreshWorker>(12, TimeUnit.HOURS).build(),
            )
        }
    }
}
