package com.adhamelgendy.nixura.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.adhamelgendy.nixura.domain.notification.NotificationActionType
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.notification.NotificationProgress
import com.adhamelgendy.nixura.domain.notification.NotificationSyncService
import com.adhamelgendy.nixura.domain.locale.ContentLanguage
import com.adhamelgendy.nixura.domain.reminder.HabitReminderCopy
import com.adhamelgendy.nixura.domain.reminder.HabitReminderCopyProvider
import com.adhamelgendy.nixura.domain.reminder.HabitReminderRules
import com.adhamelgendy.nixura.domain.reminder.ReminderActionHandler
import com.adhamelgendy.nixura.domain.reminder.ReminderActionResult
import com.adhamelgendy.nixura.domain.repository.PreferencesRepository
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.widgets.WidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext

/**
 * Runs a reminder button ("+1 glass", "Prayed", "Done", Snooze) without opening the app (phase C), with the same
 * `goAsync` pattern as the widget's "+":
 * 1. [ReminderActionHandler] writes through the widget's own use case, at most once per press (token);
 * 2. the notification is updated in place, quietly, with the native progress bar ("5 of 8 glasses"), then removes
 *    itself after a few seconds (a snooze or a met goal just says so and goes);
 * 3. widgets redraw and reminders re-plan (a logged glass cancels the pending water reminder).
 */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderActions.ACTION) return
        val token = intent.getStringExtra(ReminderActions.EXTRA_TOKEN) ?: return
        val type = intent.getStringExtra(ReminderActions.EXTRA_TYPE)?.let { n -> NotificationActionType.entries.firstOrNull { it.name == n } } ?: return
        val habitId = intent.getLongExtra(ReminderActions.EXTRA_HABIT, -1L).takeIf { it > 0 } ?: return
        val postKey = intent.getStringExtra(ReminderActions.EXTRA_POST_KEY) ?: return
        val theme = intent.getStringExtra(ReminderActions.EXTRA_THEME)?.let { n -> HabitTheme.entries.firstOrNull { it.name == n } }
        val prayer = intent.getIntExtra(ReminderActions.EXTRA_PRAYER, -1).takeIf { it > 0 }
        val snooze = intent.getIntExtra(ReminderActions.EXTRA_SNOOZE, -1).takeIf { it > 0 }
        val app = context.applicationContext
        val pending = goAsync()
        WidgetUpdater.launch {
            try {
                val koin = GlobalContext.get()
                val result = koin.get<ReminderActionHandler>().handle(token, type, habitId, prayer, snooze)
                Log.i(TAG, "action $type habit=$habitId -> $result")
                when (result) {
                    is ReminderActionResult.Logged -> {
                        showConfirmation(app, postKey, habitId, theme, copy(), result)
                        WidgetUpdater.requestRefresh(app)
                    }
                    is ReminderActionResult.Snoozed -> confirm(app, postKey, habitId, theme, copy().reminderSnoozed(result.minutes), null, progress = null)
                    // A repeated press: the first one already updated the notification and the widgets.
                    ReminderActionResult.AlreadyHandled -> Unit
                    // The habit is gone or the write failed: take the stale notification away.
                    ReminderActionResult.Failed -> NixNotifications.cancel(app, postKey)
                }
                koin.get<NotificationSyncService>().syncOnce()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "reminder action failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun copy(): HabitReminderCopy {
        val koin = GlobalContext.get()
        val settings = koin.get<PreferencesRepository>().observeSettings().first()
        return koin.get<HabitReminderCopyProvider>().forLanguage(koin.get<ContentLanguage>().update(settings.languageCode))
    }

    private fun showConfirmation(context: Context, postKey: String, habitId: Long, theme: HabitTheme?, copy: HabitReminderCopy, result: ReminderActionResult.Logged) {
        val habit = result.habit
        val target = habit.dailyTarget.coerceAtLeast(1)
        val prayer = result.prayer
        val (title, progress) = when {
            prayer != null -> copy.reminderLoggedPrayer(prayer, result.count) to NotificationProgress(result.count, HabitReminderRules.PRAYERS_PER_DAY)
            habit.theme == HabitTheme.WATER && !habit.isWeekly ->
                (if (result.count >= target) copy.reminderWaterGoalDone() else copy.reminderLoggedWater(result.count, target)) to
                    NotificationProgress(result.count, target)
            else -> copy.reminderLoggedHabit(habit.name, result.count, target) to NotificationProgress(result.count, target).takeIf { target > 1 }
        }
        confirm(context, postKey, habitId, theme ?: habit.theme, title, habit.name, progress)
    }

    /** Re-posts the same notification (same key: replaces it) without buttons or sound, and lets it go by itself. */
    private fun confirm(context: Context, postKey: String, habitId: Long, theme: HabitTheme?, title: String, body: String?, progress: NotificationProgress?) {
        NixNotifications.post(
            context,
            NixNotificationRequest(
                key = postKey,
                kind = NotificationKind.GOOD_HABIT,
                title = title,
                body = body.orEmpty(),
                targetId = habitId,
                theme = theme,
                progress = progress,
                onlyAlertOnce = true,
                timeoutAfterMillis = CONFIRMATION_MILLIS,
            ),
        )
    }

    private companion object {
        const val TAG = "NixUraReminder"

        /** How long "5 of 8 glasses" stays before it goes away (PLAN.md: about 4 s). */
        const val CONFIRMATION_MILLIS = 4_000L
    }
}
