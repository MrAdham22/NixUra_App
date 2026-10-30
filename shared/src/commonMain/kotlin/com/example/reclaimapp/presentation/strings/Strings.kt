package com.example.reclaimapp.presentation.strings

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.reclaimapp.domain.model.EventCategory

/**
 * All user-visible UI text. Defaults are English. To add a language, create another instance
 * (e.g. `val ArabicStrings = Strings(appName = "…", …)`), register it in [availableLanguages] and
 * [stringsFor]. Any member you do not override falls back to English.
 *
 * Note: the offline coach's habit knowledge base (domain/coach) is English-only for now.
 */
data class Strings(
    // Navigation
    val navHome: String = "Home",
    val navQuit: String = "Quit",
    val navBuild: String = "Build",
    val navEvents: String = "Events",
    val navCoach: String = "Coach",

    // Common
    val cancel: String = "Cancel",
    val save: String = "Save",
    val saveChanges: String = "Save changes",
    val done: String = "Done",
    val add: String = "Add",
    val delete: String = "Delete",
    val remove: String = "Remove",
    val clear: String = "Clear",
    val seeAll: String = "See all",
    val reached: String = "Reached",
    val habitNotFound: String = "This habit is no longer available.",
    val am: String = "AM",
    val pm: String = "PM",

    // Units
    val unitDays: String = "days",
    val unitHours: String = "hrs",
    val unitMinutes: String = "min",
    val unitSeconds: String = "sec",
    val weekdayLetters: List<String> = listOf("M", "T", "W", "T", "F", "S", "S"),
    val weekdayShort: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    val monthNames: List<String> = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    ),
    val monthShort: List<String> = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
    val daysCount: (Int) -> String = { if (it == 1) "1 day" else "$it days" },

    // Onboarding
    val welcomeTitle: String = "Take your life back.",
    val welcomeBody: String = "NixUra helps you leave habits behind, build better ones and look forward to what matters.",
    val welcomePoint1: String = "Track every day, hour and second since your last slip.",
    val welcomePoint2: String = "Build good habits and see your progress on a calendar.",
    val welcomePoint3: String = "Get private, on-device coaching and reminders.",
    val getStarted: String = "Get started",
    val skipForNow: String = "Skip for now",

    // Home
    val homeEyebrow: String = "Today",
    val greetingMorning: String = "Good morning",
    val greetingAfternoon: String = "Good afternoon",
    val greetingEvening: String = "Good evening",
    val todaysFocus: String = "Today's focus",
    val focusEmpty: String = "Add a habit you want to leave behind and your coach will guide you every day.",
    val openCoach: String = "Open coach",
    val momentum: String = "Momentum",
    val momentumEmpty: String = "Add a habit to start building momentum.",
    val momentumAcross: String = "Across all your habits",
    val momentumDelta: (Int) -> String = { if (it >= 0) "+$it pts vs last week" else "$it pts vs last week" },
    val quittingSection: String = "Quitting",
    val buildingSection: String = "Building",
    val nextEventSection: String = "Next event",

    // Bad habits
    val badEyebrow: String = "Break free",
    val badTitle: String = "Habits to quit",
    val badSubtitle: String = "Every second you stay clear makes you stronger. Your cards lighten as your streak grows.",
    val badEmptyTitle: String = "Nothing to quit yet",
    val badEmptyBody: String = "Tell NixUra about a habit you want to leave behind and the counter starts right away.",
    val badAdd: String = "Add a habit to quit",
    val statHabitsQuitting: String = "Quitting",
    val statBestActive: String = "Best active streak",
    val clearFor: String = "Clear for",
    val longestLabel: (String) -> String = { "Longest streak $it" },

    // Bad habit form
    val newBadHabit: String = "New habit to quit",
    val editHabit: String = "Edit habit",
    val habitNameLabel: String = "What do you want to stop?",
    val habitNamePlaceholder: String = "e.g. Smoking, soda, late-night scrolling",
    val coachUnderstood: String = "Your coach understood",
    val firstMilestone: String = "First milestone",
    val cardColor: String = "Card color",
    val colorAuto: String = "Auto",
    val cardColorHint: String = "Your card starts at a deep shade of this color and grows lighter day after day.",
    val shadeDayZero: String = "Day 0",
    val shadeDayThirty: String = "Day 30+",
    val lastTimeLabel: String = "When was the last time?",
    val startNow: String = "Just now",
    val startHourAgo: String = "1 hour ago",
    val startYesterday: String = "Yesterday",
    val startThreeDays: String = "3 days ago",
    val startWeek: String = "1 week ago",
    val startCustom: String = "Custom",
    val lastTimeHint: String = "Be honest — the counter starts from this moment.",
    val startTracking: String = "Start tracking",

    // Bad habit detail
    val nextShadeIn: (String) -> String = { "Next card shade in $it" },
    val shadeMax: String = "Your card has reached its lightest shade.",
    val statCurrent: String = "Current streak",
    val statBest: String = "Best streak",
    val statRelapses: String = "Relapses",
    val statTrackingSince: String = "Tracking since",
    val coachNote: String = "Coach note",
    val askCoach: String = "Talk to your coach",
    val timelineTitle: String = "How your body & mind recover",
    val medicalDisclaimer: String = "General wellbeing information, not medical advice. Talk to a doctor about any health concerns.",
    val relapseHistory: String = "Relapse history",
    val recordRelapse: String = "Record relapse",
    val relapseHint: String = "A slip is not failure. Recording it keeps your progress honest and restarts the counter.",
    val relapseConfirmTitle: String = "Record a relapse?",
    val relapseConfirmBody: String = "Your counter restarts from now. Your longest streak stays in your history.",
    val removeHabitTitle: String = "Remove this habit?",
    val removeHabitBody: String = "It disappears from your lists. This can't be undone.",

    // ---- Agent 1: smart recognition ----
    val looksGoodBanner: String = "This looks like a good habit. Add it to Build instead?",
    val moveToBuild: String = "Add to Build",
    val looksBadBanner: String = "This looks like a habit to quit. Track it as a quit habit instead?",
    val moveToQuit: String = "Track as quit habit",
    val didYouMean: (String) -> String = { "Did you mean \u201C$it\u201D?" },

    // Good habits
    val goodEyebrow: String = "Level up",
    val goodTitle: String = "Habits to build",
    val goodSubtitle: String = "Small actions, repeated daily. Green squares show the days you showed up.",
    val goodEmptyTitle: String = "No good habits yet",
    val goodEmptyBody: String = "Add something small — a glass of water, a short walk — and check in every day.",
    val goodAdd: String = "Add a good habit",
    val newGoodHabit: String = "New good habit",
    val goodNamePlaceholder: String = "e.g. Drink water, Morning walk",
    val quickPicks: String = "Quick picks",
    val goodSuggestions: List<Pair<String, Int>> = listOf(
        "Drink water" to 8,
        "Morning walk" to 1,
        "Workout" to 1,
        "Read 10 pages" to 1,
        "Meditate" to 1,
        "Sleep before midnight" to 1,
    ),
    val dailyTarget: String = "Daily target",
    val dailyTargetHint: String = "How many times counts as done for the day",
    val addHabit: String = "Add habit",
    val todayLabel: String = "Today",
    val habitsDoneToday: (Int, Int) -> String = { done, total -> "$done of $total done" },
    val todayProgress: (Int, Int) -> String = { count, target -> "$count / $target today" },
    val streakDays: (Int) -> String = { if (it == 1) "1 day streak" else "$it day streak" },
    val timesDoneTotal: (Int) -> String = { "$it times in total" },
    val goalReached: String = "Daily goal reached",
    val timesToGo: (Int) -> String = { if (it == 1) "1 more to go" else "$it more to go" },
    val statCurrentStreak: String = "Current streak",
    val statBestStreak: String = "Best streak",
    val statTotalTimes: String = "Times done",
    val statDaysCompleted: String = "Days completed",
    val last7Days: String = "Last 7 days",
    val last30Days: String = "Last 30 days",
    val calendarTitle: String = "Calendar",
    val legendDone: String = "Done",
    val legendPartial: String = "Partly",
    val legendMissed: String = "Missed",
    val editPastHint: String = "Forgot to log a day? Adjust it here.",
    val removeGoodHabitBody: String = "It disappears from your lists, along with its history. This can't be undone.",

    // Events
    val eventsEyebrow: String = "Look forward",
    val eventsTitle: String = "Important events",
    val eventsSubtitle: String = "Live countdowns to the moments that matter, with reminders when you want them.",
    val eventsEmptyTitle: String = "No events yet",
    val eventsEmptyBody: String = "Add a trip, a deadline or a celebration and watch the countdown.",
    val eventsAdd: String = "Add an event",
    val upcoming: String = "Upcoming",
    val pastEvents: String = "Past events",
    val startsIn: String = "Starts in",
    val passedSince: String = "Passed since",
    val newEvent: String = "New event",
    val editEvent: String = "Edit event",
    val eventNameLabel: String = "Event name",
    val eventNamePlaceholder: String = "e.g. Trip to Siwa",
    val eventCategoryLabel: String = "Category",
    val eventWhenLabel: String = "Date & time",
    val eventPastWarning: String = "This moment has already passed — reminders won't be sent.",
    val remindMe: String = "Remind me",
    val customReminder: String = "Custom reminder",
    val reminderHint: String = "Pick as many reminders as you like. They arrive as notifications on this device.",
    val addEvent: String = "Add event",
    val deleteEvent: String = "Delete event",
    val deleteEventTitle: String = "Delete this event?",
    val deleteEventBody: String = "The countdown and its reminders will be removed.",
    val atEventTime: String = "At event time",
    val minutesBefore: (Int) -> String = { "$it min before" },
    val hoursBefore: (Int) -> String = { if (it == 1) "1 hour before" else "$it hours before" },
    val daysBefore: (Int) -> String = { if (it == 1) "1 day before" else "$it days before" },
    val categoryLabel: (EventCategory) -> String = {
        when (it) {
            EventCategory.PERSONAL -> "Personal"
            EventCategory.TRAVEL -> "Travel"
            EventCategory.WORK -> "Work"
            EventCategory.HEALTH -> "Health"
            EventCategory.CELEBRATION -> "Celebration"
            EventCategory.OTHER -> "Other"
        }
    },

    // Coach
    val coachEyebrow: String = "Your coach",
    val coachTitle: String = "AI Coach",
    val coachInputHint: String = "Ask for help, a plan or motivation…",
    val coachOfflineNote: String = "Replies are generated on your device. Private, but not a substitute for professional care.",
    val coachModeOfflineShort: String = "Offline · private, on your device",
    val coachPickHabit: String = "Choose a habit to talk about",
    val clearChatTitle: String = "Clear the conversation?",
    val clearChatBody: String = "Your chat history will be deleted. Your habits stay untouched.",

    // Profile
    val profileTitle: String = "Profile",
    val profileNoName: String = "Add your name",
    val editProfile: String = "Edit profile",
    val nameLabel: String = "Name",
    val namePlaceholder: String = "What should we call you?",
    val emailLabel: String = "Email",
    val ageLabel: String = "Age",
    val ageYears: (Int) -> String = { "$it years old" },
    val yourProgress: String = "Your progress",
    val quitScoreTitle: String = "Breaking free",
    val quitScoreBody: String = "How far you are toward 30 days clear, on average",
    val disciplineScoreTitle: String = "Discipline",
    val disciplineScoreBody: String = "Your good-habit completion over the last 7 days",
    val noBadHabitsYet: String = "Add a habit to quit to see this score.",
    val noGoodHabitsYet: String = "Add a good habit to see this score.",
    val statClearDays: String = "Clear days",
    val statCheckIns: String = "Check-ins",
    val statHabitsTracked: String = "Habits tracked",
    val nearestEvent: String = "Nearest event",
    val noUpcomingEvents: String = "No upcoming events. Tap to add one.",

    // Settings
    val settingsTitle: String = "Settings",
    val appearance: String = "Appearance",
    val darkMode: String = "Dark",
    val lightMode: String = "Light",
    val language: String = "Language",
    val notifications: String = "Notifications",
    val notificationsToggle: String = "Reminders & coaching",
    val notificationsHint: String = "Event reminders, streak milestones and a daily check-in.",
    val dailyCheckIn: String = "Daily check-in",
    val dailyCheckInHint: String = "A short note from your coach",
    val dataTitle: String = "Your data",
    val dataBody: String = "Everything is stored only on this device. There are no accounts and nothing is uploaded.",
    val eraseData: String = "Erase all data",
    val eraseTitle: String = "Erase everything?",
    val eraseBody: String = "All habits, events, chats and settings will be permanently deleted from this device.",
    val eraseConfirm: String = "Erase",
    val about: String = "About",
    val versionLabel: String = "Version 1.0",
    val aboutBody: String = "NixUra helps you break bad habits, build good ones and stay on track for the moments that matter.",
)

data class AppLanguage(val code: String, val nativeName: String)

val availableLanguages: List<AppLanguage> = listOf(
    AppLanguage("en", "English"),
)

val EnglishStrings = Strings()

fun stringsFor(code: String): Strings = when (code) {
    else -> EnglishStrings
}

val LocalStrings = staticCompositionLocalOf { EnglishStrings }
