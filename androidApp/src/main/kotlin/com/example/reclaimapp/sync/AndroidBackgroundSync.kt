package com.example.reclaimapp.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.reclaimapp.data.auth.SessionManagerImpl
import com.example.reclaimapp.domain.integration.SyncIntegrationsUseCase
import com.example.reclaimapp.domain.notification.NotificationSyncService
import com.example.reclaimapp.domain.sync.BackgroundSyncScheduler
import com.example.reclaimapp.domain.sync.SyncEngine
import com.example.reclaimapp.domain.sync.SyncOutcome
import org.koin.core.context.GlobalContext
import java.util.concurrent.TimeUnit

/** Syncs when the app is closed. Pulled changes also need local alarms re-armed, since alarms are per device. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val koin = GlobalContext.get()
        koin.get<SessionManagerImpl>().restore()
        // Linked calendars first, so the cloud sync below also carries what was just imported.
        runCatching {
            koin.get<SyncIntegrationsUseCase>().invoke(skipIfSyncedWithinMillis = SyncIntegrationsUseCase.AUTO_SYNC_EVERY_MILLIS)
        }
        return when (koin.get<SyncEngine>().syncNow()) {
            SyncOutcome.Synced -> {
                koin.get<NotificationSyncService>().syncOnce()
                Result.success()
            }
            SyncOutcome.Offline, SyncOutcome.Failed -> Result.retry()
            SyncOutcome.NotSignedIn, SyncOutcome.SessionEnded -> Result.success()
        }
    }
}

class AndroidBackgroundSync(context: Context) : BackgroundSyncScheduler {
    private val work = WorkManager.getInstance(context.applicationContext)
    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    override fun schedulePeriodic() {
        work.enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).setConstraints(online).build(),
        )
    }

    override fun scheduleRetry() {
        work.enqueueUniqueWork(
            RETRY,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(online)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build(),
        )
    }

    override fun cancelAll() {
        work.cancelUniqueWork(PERIODIC)
        work.cancelUniqueWork(RETRY)
    }

    private companion object {
        const val PERIODIC = "nixura-sync-periodic"
        const val RETRY = "nixura-sync-retry"
    }
}
