package com.adhamelgendy.nixura.domain.notification

/** How loud a channel is when it is first created. After that only the user can change it (Android rule). */
enum class ChannelImportance {
    /** Silent, shown in the shade only. For things that are nice to know, not time-critical. */
    LOW,

    /** Sound / vibration per the phone's settings. For things tied to a moment (an event, a habit's time). */
    DEFAULT,
}

/** The sections the channels are listed under in the system's notification settings. IDs are permanent. */
enum class NixChannelGroup(val id: String) {
    HABITS("nixura_group_habits"),
    EVENTS("nixura_group_events"),
    COACH_AND_APP("nixura_group_coach_app"),
}

/**
 * The app's notification channels. Kept small and user-meaningful: each one is a switch the user may want on its
 * own. **IDs are permanent once a build with them is released** (Android keeps the user's choices per ID).
 *
 * [EVENTS] keeps the pre-phase-B ID `nixura_reminders` (it already carried event reminders), so existing
 * installs keep their channel settings; only its name changed ("Reminders & coaching" → "Events").
 */
enum class NixChannel(
    val id: String,
    val group: NixChannelGroup,
    val importance: ChannelImportance,
    /** Created with no sound and no vibration, whatever its importance. */
    val silent: Boolean = false,
) {
    EVENTS("nixura_reminders", NixChannelGroup.EVENTS, ChannelImportance.DEFAULT),
    GOOD_HABITS("nixura_good_habits", NixChannelGroup.HABITS, ChannelImportance.DEFAULT),
    QUIT_PROGRESS("nixura_quit_progress", NixChannelGroup.HABITS, ChannelImportance.LOW),
    COACH("nixura_coach", NixChannelGroup.COACH_AND_APP, ChannelImportance.LOW),
    APP("nixura_app", NixChannelGroup.COACH_AND_APP, ChannelImportance.LOW),

    /** DEFAULT, not LOW: phones can keep LOW notifications off the lock screen, the one place this is for. */
    TODAY("nixura_today", NixChannelGroup.HABITS, ChannelImportance.DEFAULT, silent = true),
}

/** Pure mapping from a notification's kind to where and how it is posted. Shared so iOS can mirror it. */
object NotificationChannels {

    /** Channels that existed once and must be deleted on start. None yet: nothing has been retired. */
    val RETIRED_CHANNEL_IDS: List<String> = emptyList()

    fun channelFor(kind: NotificationKind): NixChannel = when (kind) {
        NotificationKind.EVENT -> NixChannel.EVENTS
        NotificationKind.GOOD_HABIT -> NixChannel.GOOD_HABITS
        NotificationKind.QUIT_MILESTONE -> NixChannel.QUIT_PROGRESS
        NotificationKind.COACH -> NixChannel.COACH
        NotificationKind.APP -> NixChannel.APP
        NotificationKind.TODAY -> NixChannel.TODAY
    }

    /**
     * Group key per kind: notifications of one kind bundle together in the shade instead of mixing with other
     * kinds (Android `setGroup`, iOS `threadIdentifier`).
     */
    fun groupKeyFor(kind: NotificationKind): String = "com.adhamelgendy.nixura.${kind.name.lowercase()}"
}
