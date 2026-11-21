package com.adhamelgendy.nixura.domain.notification

import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.locale.ContentLanguage
import com.adhamelgendy.nixura.domain.reminder.HabitReminderPlanner
import com.adhamelgendy.nixura.domain.reminder.ReminderDeviceRepository
import com.adhamelgendy.nixura.domain.reminder.ReminderDeviceState
import com.adhamelgendy.nixura.domain.repository.EventRepository
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.repository.HabitTrackingRepository
import com.adhamelgendy.nixura.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

/**
 * Keeps the OS-level scheduled notifications in sync with app data.
 * Runs while the app UI is open ([run]); background paths (a widget "+", a notification button, a delivered good-habit
 * reminder, the daily refresh, boot / clock changes) call [syncOnce]. The OS then fires the notifications on its own.
 */
class NotificationSyncService(
    private val tracking: HabitTrackingRepository,
    private val goodHabits: GoodHabitRepository,
    private val events: EventRepository,
    private val preferences: PreferencesRepository,
    private val planner: NotificationPlanner,
    private val scheduler: NotificationScheduler,
    private val clock: Clock,
    /** Re-plans when the shown language changes (the texts are written in it). */
    private val language: ContentLanguage = ContentLanguage.English,
    /** Good-habit reminders (phase C); `null` in tests that only cover events, milestones and the check-in. */
    private val reminderPlanner: HabitReminderPlanner? = null,
    private val reminderDevice: ReminderDeviceRepository? = null,
) {
    suspend fun run() {
        plans().distinctUntilChanged().collect { scheduler.replaceAll(it) }
    }

    /** Re-arms once, for background work that changed data while no screen is open (alarms are per device). */
    suspend fun syncOnce() {
        scheduler.replaceAll(plans().first())
    }

    private fun plans(): Flow<List<PlannedNotification>> = combine(
        tracking.observeActiveHabitTracking(),
        goodHabits.observeGoodHabits(),
        events.observeEvents(),
        combine(preferences.observeSettings(), language.current) { settings, _ -> settings },
        combine(preferences.observeProfile(), reminderDevice?.observe() ?: flowOf(ReminderDeviceState())) { profile, device -> profile to device },
    ) { bad, good, upcoming, settings, (profile, device) ->
        val now = clock.nowEpochMillis()
        val base = planner.plan(now, settings, profile.name, bad, good, upcoming)
        // The app-wide switch in Settings turns habit reminders off too.
        val reminders = if (settings.notificationsEnabled && reminderPlanner != null) {
            reminderPlanner.plan(now, language.update(settings.languageCode), good, device)
        } else {
            emptyList()
        }
        (base + reminders).sortedBy { it.triggerAtEpochMillis }
    }
}
