package com.adhamelgendy.nixura.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.adhamelgendy.nixura.domain.notification.ChannelImportance
import com.adhamelgendy.nixura.domain.notification.NixChannel
import com.adhamelgendy.nixura.domain.notification.NixChannelGroup
import com.adhamelgendy.nixura.domain.notification.NotificationChannels
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.notification.NotificationProgress
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.recognition.HabitTopic
import com.adhamelgendy.nixura.domain.notification.LockScreenPrivacy
import com.adhamelgendy.nixura.domain.notification.LockScreenVisibility
import android.os.Bundle
import com.adhamelgendy.nixura.domain.notification.HiddenNameWithdrawal
import com.adhamelgendy.nixura.domain.notification.ShownName
import com.adhamelgendy.nixura.domain.notification.TodayMetric
import com.adhamelgendy.nixura.domain.notification.TodayClock

/**
 * What to show. Built by the scheduler's receiver today; phase C builds it for good-habit reminders.
 *
 * @param key stable identity: posting the same key again **replaces** the notification (e.g. one water reminder per
 *   habit, never a stack). Use the planner's key.
 * @param targetId the event or habit a tap opens (see [NotificationDeepLink]).
 * @param actions buttons (phase C: "+1 glass", "Prayed"). Each one's PendingIntent must target a non-exported receiver
 *   (see [ReminderActions]).
 */
data class NixNotificationRequest(
    val key: String,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val targetId: Long? = null,
    val theme: HabitTheme? = null,
    /** A good habit's kind, for the card glyph as small icon (see [NixNotificationStyles.of]). */
    val topic: HabitTopic? = null,
    val actions: List<NotificationCompat.Action> = emptyList(),
    /** Native progress bar ("3 / 8 glasses"); `null` = none. Never an animation: the app shows that on tap. */
    val progress: NotificationProgress? = null,
    /** True for an update of a notification already showing (after a button): no second sound or vibration. */
    val onlyAlertOnce: Boolean = false,
    /** Removes the notification by itself after this long (the "5 of 8 glasses" confirmation); `null` = stays. */
    val timeoutAfterMillis: Long? = null,
    /** Told when the user swipes it away; `null` = nothing to tell. */
    val deleteIntent: PendingIntent? = null,
    /** The neutral line shown instead on a lock screen that hides sensitive content; `null` = the app's name only. */
    val publicTitle: String? = null,
    /** The quit habit whose name the text shows (see [NixNotifications.withdrawNamesOf]); `null` = none. */
    val namedHabitId: Long? = null,
    /** Today only: stays against "Clear all" and while locked. Not pinned: Android 14+ lets the user swipe it away. */
    val ongoing: Boolean = false,
    /** A countdown to this moment, drawn and ticked by the system. */
    val countdownTo: Long? = null,
    val countdownLabel: String? = null,
    val countdownClock: TodayClock? = null,
    /** The expanded text when it says more than [body]. */
    val expandedText: String? = null,
    /** Today only: shown with Android 17's `MetricStyle`, where there is one. */
    val metrics: List<TodayMetric> = emptyList(),
)

/**
 * The one place the app builds Android notifications. Every phase posts through [post]; nobody calls
 * `NotificationCompat.Builder` or creates channels elsewhere. It owns:
 * - the channels and channel groups (from the shared [NotificationChannels] catalog, names from Android resources),
 * - the look per kind ([NixNotificationStyles]: small icon, accent colour, category, priority),
 * - grouping (one group per kind, with a summary once two or more of that kind are showing),
 * - what a locked screen shows ([LockScreenPrivacy]), set on every notification and every summary,
 * - the tap target ([NotificationDeepLink]).
 */
object NixNotifications {

    /**
     * Creates or updates every channel. Safe to call often: re-creating an existing channel only refreshes its name
     * and description (so they follow the app's language); importance and the user's choices are never touched.
     */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannelGroups(
            NixChannelGroup.entries.map { NotificationChannelGroup(it.id, context.getString(NixNotificationStyles.nameOf(it))) },
        )
        manager.createNotificationChannels(
            NixChannel.entries.map { channel ->
                NotificationChannel(channel.id, context.getString(NixNotificationStyles.nameOf(channel)), channel.importance.toAndroid()).apply {
                    description = context.getString(NixNotificationStyles.descriptionOf(channel))
                    group = channel.group.id
                    if (channel.silent) {
                        setSound(null, null)
                        enableVibration(false)
                        setShowBadge(false)
                    }
                }
            },
        )
        NotificationChannels.RETIRED_CHANNEL_IDS.forEach(manager::deleteNotificationChannel)
    }

    /** Shows [request] now. Does nothing (and never throws) when notifications are not allowed. */
    fun post(context: Context, request: NixNotificationRequest) {
        if (!canPost(context)) return
        ensureChannels(context)
        val channel = NotificationChannels.channelFor(request.kind)
        val style = NixNotificationStyles.of(request.kind, request.theme, request.topic)
        val group = NotificationChannels.groupKeyFor(request.kind)
        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(NotificationIconPainter.smallIcon(context, style))
            .setColor(style.accent)
            .setCategory(style.category)
            .setContentTitle(request.title)
            .setContentText(request.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(request.expandedText ?: request.body))
            .setPriority(channel.importance.toCompatPriority()) // Android 7 and older have no channels
            .setGroup(group)
            .setAutoCancel(!request.ongoing)
            .setOngoing(request.ongoing)
            .setContentIntent(NotificationDeepLink.contentIntent(context, request.key, request.kind, request.targetId))
            .setOnlyAlertOnce(request.onlyAlertOnce)
            .lockScreen(context, request.kind, channel, style, request.publicTitle)
            .addExtras(namesExtra(request.namedHabitId))
            .apply {
                request.actions.forEach(::addAction)
                request.progress?.let { setProgress(it.max.coerceAtLeast(1), it.current.coerceIn(0, it.max.coerceAtLeast(1)), false) }
                request.timeoutAfterMillis?.let(::setTimeoutAfter)
                request.deleteIntent?.let(::setDeleteIntent)
                if (request.countdownTo != null) {
                    setWhen(request.countdownTo)
                    setShowWhen(true)
                    setUsesChronometer(true)
                    setChronometerCountDown(true)
                } else if (request.ongoing) {
                    setShowWhen(false) // "3 h ago" says nothing about a notification that is always current
                }
            }
            .build()
            .let { if (Build.VERSION.SDK_INT >= 37 && request.metrics.isNotEmpty()) TodayMetricStyle.applyTo(context, it, request) else it }
        val manager = NotificationManagerCompat.from(context)
        try {
            manager.notify(idOf(request.key), notification)
            postSummaryIfGrouped(context, request.kind, channel)
        } catch (denied: SecurityException) {
            // Permission revoked between the check and the call: nothing to show.
        }
    }

    /** Removes a posted notification (phase C: after "+1 glass" completes the goal). */
    fun cancel(context: Context, key: String) {
        NotificationManagerCompat.from(context).cancel(idOf(key))
    }

    /**
     * "Hide name outside the app" was turned on: takes away what is already showing that name (a milestone, a
     * check-in), and a bundle left with nothing in it. What is showing without the name stays.
     */
    fun withdrawNamesOf(context: Context, hiddenHabitIds: Set<Long>) {
        if (hiddenHabitIds.isEmpty()) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val showing = runCatching { manager.activeNotifications.toList() }.getOrDefault(emptyList())
        for (kind in NotificationKind.entries) {
            val group = NotificationChannels.groupKeyFor(kind)
            val summaryId = idOf(SUMMARY_PREFIX + group)
            val children = showing.filter { it.notification.group == group && it.id != summaryId }
            val removed = children.filter { HiddenNameWithdrawal.shouldRemove(kind, shownNameOf(it.notification.extras), hiddenHabitIds) }
            removed.forEach { manager.cancel(it.tag, it.id) }
            if (removed.isNotEmpty() && removed.size == children.size) manager.cancel(summaryId)
        }
    }

    private fun namesExtra(habitId: Long?): Bundle = Bundle().apply { putLong(EXTRA_NAMES_HABIT, habitId ?: NAMES_NOBODY) }

    private fun shownNameOf(extras: Bundle): ShownName {
        if (!extras.containsKey(EXTRA_NAMES_HABIT)) return ShownName.Unknown
        val id = extras.getLong(EXTRA_NAMES_HABIT)
        return if (id == NAMES_NOBODY) ShownName.None else ShownName.Of(id)
    }

    /** False when the user switched [kind]'s channel off (or all of the app's notifications). */
    fun channelEnabled(context: Context, kind: NotificationKind): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < 26) return true
        val channel = manager.getNotificationChannel(NotificationChannels.channelFor(kind).id) ?: return true // not created yet = default on
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** The system screen of [kind]'s channel; on Android 7 and older the app's notification screen. */
    fun channelSettingsIntent(context: Context, kind: NotificationKind): Intent =
        if (Build.VERSION.SDK_INT >= 26) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannels.channelFor(kind).id)
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

    /** The OS notification id. Same key → same id → replaces. Matches the pre-phase-B id, so nothing duplicates. */
    fun idOf(key: String): Int = key.hashCode()

    private fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Android only bundles a group when a summary exists. Post one once two or more of this kind are showing, so a
     * lone notification stays a plain notification. The summary itself never alerts (children do).
     */
    private fun postSummaryIfGrouped(context: Context, kind: NotificationKind, channel: NixChannel) {
        if (Build.VERSION.SDK_INT < 24) return
        val group = NotificationChannels.groupKeyFor(kind)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val summaryId = idOf(SUMMARY_PREFIX + group)
        val children = manager.activeNotifications.count { it.notification.group == group && it.id != summaryId }
        if (children < 2) return
        val style = NixNotificationStyles.of(kind) // the bundle stands for several: the leaf, not the newest one's glyph
        val summary = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(style.smallIcon)
            .setColor(style.accent)
            .setCategory(style.category)
            .setContentTitle(context.getString(NixNotificationStyles.nameOf(channel)))
            .setGroup(group)
            .setGroupSummary(true)
            .addExtras(namesExtra(null)) // a bundle's own text is the type's name
            .lockScreen(context, kind, channel, style, context.getString(NixNotificationStyles.nameOf(channel)))
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .setContentIntent(NotificationDeepLink.contentIntent(context, SUMMARY_PREFIX + group, kind, targetId = null))
            .build()
        NotificationManagerCompat.from(context).notify(summaryId, summary)
    }

    /**
     * What a locked screen shows. It must be on each notification: Android resets an app's channel-level value.
     * The public version is used only where the phone hides sensitive content.
     */
    private fun NotificationCompat.Builder.lockScreen(
        context: Context,
        kind: NotificationKind,
        channel: NixChannel,
        style: NixNotificationStyle,
        publicTitle: String?,
    ): NotificationCompat.Builder {
        if (LockScreenPrivacy.visibilityOf(kind) == LockScreenVisibility.PUBLIC) {
            return setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }
        setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        if (publicTitle == null) return this
        val neutral = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(style.smallIcon) // the type's own icon, never a habit's
            .setColor(style.accent)
            .setCategory(style.category)
            .setContentTitle(publicTitle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        return setPublicVersion(neutral)
    }

    private fun ChannelImportance.toAndroid(): Int = when (this) {
        ChannelImportance.LOW -> NotificationManager.IMPORTANCE_LOW
        ChannelImportance.DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
    }

    private fun ChannelImportance.toCompatPriority(): Int = when (this) {
        ChannelImportance.LOW -> NotificationCompat.PRIORITY_LOW
        ChannelImportance.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
    }

    private const val SUMMARY_PREFIX = "summary:"
    private const val EXTRA_NAMES_HABIT = "nixura.notification.names_habit"
    private const val NAMES_NOBODY = -1L
}
