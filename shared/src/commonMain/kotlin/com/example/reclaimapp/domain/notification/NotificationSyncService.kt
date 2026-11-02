package com.example.reclaimapp.domain.notification

import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.repository.EventRepository
import com.example.reclaimapp.domain.repository.GoodHabitRepository
import com.example.reclaimapp.domain.repository.HabitTrackingRepository
import com.example.reclaimapp.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/**
 * Keeps the OS-level scheduled notifications in sync with app data.
 * Runs while the app UI is open; the OS then fires the notifications on its own (also after reboot).
 */
class NotificationSyncService(
    private val tracking: HabitTrackingRepository,
    private val goodHabits: GoodHabitRepository,
    private val events: EventRepository,
    private val preferences: PreferencesRepository,
    private val planner: NotificationPlanner,
    private val scheduler: NotificationScheduler,
    private val clock: Clock,
) {
    suspend fun run() {
        plans().distinctUntilChanged().collect { scheduler.replaceAll(it) }
    }

    /** Re-arms once, for background work that changed data while no screen is open (alarms are per device). */
    suspend fun syncOnce() {
        scheduler.replaceAll(plans().first())
    }

    private fun plans() = combine(
        tracking.observeActiveHabitTracking(),
        goodHabits.observeGoodHabits(),
        events.observeEvents(),
        preferences.observeSettings(),
        preferences.observeProfile(),
    ) { bad, good, upcoming, settings, profile ->
        planner.plan(clock.nowEpochMillis(), settings, profile.name, bad, good, upcoming)
    }
}
