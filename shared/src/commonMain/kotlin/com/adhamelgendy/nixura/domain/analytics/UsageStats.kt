package com.adhamelgendy.nixura.domain.analytics

/**
 * Anonymous app-usage statistics (Firebase Analytics on Android). On by default (owner decision 2026-11-23); the user
 * can turn it off in Settings > Privacy. Nothing the user typed (habit names, notes, chats), no ids, no emails and no
 * habit categories or themes (an addiction or a prayer habit is sensitive) are ever sent: only screen names and the
 * fixed [UsageAction] names below.
 */
interface UsageStats {
    fun setCollectionEnabled(enabled: Boolean)

    /** A screen was shown. [screen] is a fixed name such as "home" or "good_habit_detail" (see presentation/navigation/ScreenNames.kt). */
    fun logScreen(screen: String) = Unit

    /** The user did one of the main things in the app. */
    fun logAction(action: UsageAction) = Unit
}

/** The main actions counted in usage stats. The names are what shows in the Firebase console (Events). */
enum class UsageAction(val eventName: String) {
    QuitHabitCreated("quit_habit_created"),
    RelapseLogged("relapse_logged"),
    GoodHabitCreated("good_habit_created"),
    GoodHabitLogged("good_habit_logged"),
    GoodHabitUnlogged("good_habit_unlogged"),
    EventCreated("event_created"),
    CoachMessageSent("coach_message_sent"),
    CravingHelpUsed("craving_help_used"),
    VoiceNoteAdded("voice_note_added"),
}

/** Platforms without an analytics SDK (iOS for now) collect nothing. */
object NoUsageStats : UsageStats {
    override fun setCollectionEnabled(enabled: Boolean) = Unit
}
