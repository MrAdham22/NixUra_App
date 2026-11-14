package com.adhamelgendy.nixura.presentation.strings

import com.adhamelgendy.nixura.domain.money.CostUnit
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.adhamelgendy.nixura.domain.model.EventCategory

/**
 * All user-visible UI text. Defaults are English. To add a language, subclass it and override only
 * what differs (e.g. `object ArabicStrings : Strings() { override val navHome = "…" }`), then register
 * it in [availableLanguages] and [stringsFor]. Any member you do not override falls back to English.
 *
 * Why this is a plain class with body properties and NOT a `data class` with one constructor
 * parameter per string: with ~256 parameters the compiler generates a synthetic default-constructor
 * call with ~265 arguments for `Strings()`. D8 mis-encodes such a huge invoke and Android's verifier
 * rejects `StringsKt.<clinit>` with `VerifyError: expected N argument registers, method signature has
 * 11 or more`. Keep every new string as an `open val` in the body; never move them back into a
 * constructor.
 *
 * Note: the offline coach's habit knowledge base (domain/coach) is English-only for now.
 */
@Immutable
open class Strings {
    // Navigation
    open val navHome: String = "Home"
    open val navQuit: String = "Quit"
    open val navBuild: String = "Build"
    open val navEvents: String = "Events"
    open val navCoach: String = "Coach"

    // Common
    open val cancel: String = "Cancel"
    open val save: String = "Save"
    open val saveChanges: String = "Save changes"
    open val done: String = "Done"
    open val add: String = "Add"
    open val delete: String = "Delete"
    open val remove: String = "Remove"
    open val seeAll: String = "See all"
    open val reached: String = "Reached"
    open val habitNotFound: String = "This habit is no longer available."
    open val am: String = "AM"
    open val pm: String = "PM"

    // Units
    open val unitDays: String = "days"
    open val unitHours: String = "hrs"
    open val unitMinutes: String = "min"
    open val unitSeconds: String = "sec"
    open val weekdayLetters: List<String> = listOf("M", "T", "W", "T", "F", "S", "S")
    open val weekdayShort: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    open val monthNames: List<String> = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
    open val monthShort: List<String> = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    open val daysCount: (Int) -> String = { if (it == 1) "1 day" else "$it days" }

    // Onboarding

    // Home
    open val greetingMorning: String = "Good morning"
    open val greetingAfternoon: String = "Good afternoon"
    open val greetingEvening: String = "Good evening"
    open val todaysFocus: String = "Today's focus"
    open val momentum: String = "Momentum"
    open val momentumDelta: (Int) -> String = { if (it >= 0) "+$it pts vs last week" else "$it pts vs last week" }
    open val quittingSection: String = "Quitting"
    open val buildingSection: String = "Building"
    open val nextEventSection: String = "Next event"

    // Bad habits
    open val badEyebrow: String = "Break free"
    open val badTitle: String = "Habits to quit"
    open val badSubtitle: String = "Your cards lighten as your streak grows."
    open val badEmptyTitle: String = "Nothing to quit yet"
    open val badEmptyBody: String = "Tell NixUra about a habit you want to leave behind and the counter starts right away."
    open val badAdd: String = "Add a habit to quit"
    open val statHabitsQuitting: String = "Quitting"
    open val statBestActive: String = "Best active streak"
    open val clearFor: String = "Clear for"
    open val longestLabel: (String) -> String = { "Longest streak $it" }
    open val longestYet: String = "Your longest streak yet"
    open val cravingAction: String = "Craving? Talk it through"
    open val cravingShort: String = "Craving?"
    /** A counter read aloud by a screen reader: "3 days, 4 hours, 56 minutes, 12 seconds". */
    open fun counterSpoken(days: Long?, hours: Long?, minutes: Long?, seconds: Long?): String = listOfNotNull(
        days?.let { if (it == 1L) "1 day" else "$it days" },
        hours?.let { if (it == 1L) "1 hour" else "$it hours" },
        minutes?.let { if (it == 1L) "1 minute" else "$it minutes" },
        seconds?.let { if (it == 1L) "1 second" else "$it seconds" },
    ).joinToString(", ")
    open fun passedAgo(duration: String): String = "$duration ago"
    open val slipAction: String = "I slipped"
    open val cardMoreOptions: String = "More options"
    open fun cravingPrompt(habit: String): String = "I'm having a craving for $habit right now. Help me get through it."
    open val statNextWin: String = "Next win"
    open val statSaved: String = "Saved"
    open val infoNextWin: String = "The closest milestone among your quit habits. Hold on until then: every milestone you reach is a win, and a slip never erases the ones behind you."
    open fun nextWinLine(habit: String, left: String): String = "$habit · in $left"
    open fun nextMilestone(milestone: String, left: String): String = "Next: $milestone · $left"
    open fun stageOf(stage: Int, count: Int): String = "Stage $stage of $count"
    open fun milestoneName(days: Int): String = when {
        days == 1 -> "1 day"
        days < 7 -> "$days days"
        days == 7 -> "1 week"
        days < 30 && days % 7 == 0 -> "${days / 7} weeks"
        days == 30 -> "1 month"
        days < 365 && days % 30 == 0 -> "${days / 30} months"
        days == 180 -> "6 months"
        days == 365 -> "1 year"
        days % 365 == 0 -> "${days / 365} years"
        else -> "$days days"
    }
    /** Time left, short: "3d 20h", "5h 12m", "12m", "under 1m". */
    open fun timeLeftShort(days: Long, hours: Long, minutes: Long): String = when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "under 1m"
    }

    // Bad habit form
    open val newBadHabit: String = "New habit to quit"
    open val editHabit: String = "Edit habit"
    open val habitNameLabel: String = "What do you want to stop?"
    open val goodHabitNameLabel: String = "What do you want to build?"
    open val habitNamePlaceholder: String = "e.g. Smoking, soda, late-night scrolling"
    open val coachUnderstood: String = "Your coach understood"
    open val firstMilestone: String = "First milestone"
    open val cardColor: String = "Card color"
    open val colorAuto: String = "Auto"
    open val cardColorHint: String = "Your card starts at a deep shade of this color and grows lighter day after day."
    open val shadeDayZero: String = "Day 0"
    open val shadeDayThirty: String = "Day 30+"
    open val lastTimeLabel: String = "When was the last time?"
    open val startNow: String = "Just now"
    open val startHourAgo: String = "1 hour ago"
    open val startYesterday: String = "Yesterday"
    open val startThreeDays: String = "3 days ago"
    open val startWeek: String = "1 week ago"
    open val startCustom: String = "Custom"
    open val lastTimeHint: String = "Be honest — the counter starts from this moment."
    open val startTracking: String = "Start tracking"

    // Bad habit detail
    open val nextShadeIn: (String) -> String = { "Next card shade in $it" }
    open val shadeMax: String = "Your card has reached its lightest shade."
    open val statCurrent: String = "Current streak"
    open val statBest: String = "Best streak"
    open val statRelapses: String = "Relapses"
    open val statTrackingSince: String = "Tracking since"
    open val coachNote: String = "Coach note"
    open val askCoach: String = "Talk to your coach"
    open val timelineTitle: String = "How your body & mind recover"
    open val timelineMilestones: String = "Milestones"
    open fun timelineReachedCount(reached: Int, total: Int): String = "$reached of $total reached"
    open val timelineUpNext: String = "Up next"
    open fun timelineIn(time: String): String = "in $time"
    open fun timelineAfter(time: String): String = "After $time"
    open val timelineAllReached: String = "Every milestone reached. Keep it going."
    open fun timelineShowEarlier(count: Int): String = if (count == 1) "Show 1 earlier milestone" else "Show $count earlier milestones"
    open val timelineHideEarlier: String = "Hide earlier milestones"
    open fun timelineEarlierCount(count: Int): String = "+$count"
    open val medicalDisclaimer: String = "General wellbeing information, not medical advice. Talk to a doctor about any health concerns."
    open val relapseHistory: String = "Relapse history"
    open val recordRelapse: String = "Record relapse"
    open val relapseHint: String = "A slip is not failure. Recording it keeps your progress honest and restarts the counter."
    open val relapseConfirmTitle: String = "Record a relapse?"
    open val relapseConfirmBody: String = "Your counter restarts from now. Your longest streak stays in your history."

    // ---- Agent 1: smart recognition ----
    open val looksGoodBanner: String = "This looks like a good habit. Add it to Build instead?"
    open val moveToBuild: String = "Add to Build"
    open val looksBadBanner: String = "This looks like a habit to quit. Track it as a quit habit instead?"
    open val moveToQuit: String = "Track as quit habit"
    open val didYouMean: (String) -> String = { "Did you mean \u201C$it\u201D?" }

    // Good habits
    open val goodEyebrow: String = "Level up"
    open val goodTitle: String = "Habits to build"
    open val goodSubtitle: String = "Small actions, repeated daily."
    open val goodEmptyTitle: String = "No good habits yet"
    open val goodEmptyBody: String = "Add something small — a glass of water, a short walk — and check in every day."
    open val goodAdd: String = "Add a good habit"
    open val newGoodHabit: String = "New good habit"
    open val goodNamePlaceholder: String = "e.g. Drink water, Morning walk"
    open val quickPicks: String = "Quick picks"
    open val goodSuggestions: List<Pair<String, Int>> = listOf(
        "Drink water" to 8,
        "Morning walk" to 1,
        "Workout" to 1,
        "Meditate" to 1,
        "Pray" to 5,
    )
    /** Shown on the stars of a five-a-day prayer habit. */
    open val prayerNames: List<String> = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    open val prayerHint: String = "Set to the five daily prayers. Change the number whenever you like."
    open val dailyTarget: String = "Daily target"
    open val dailyTargetHint: String = "How many times counts as done for the day"
    open val addHabit: String = "Add habit"
    open val todayLabel: String = "Today"
    open val habitsDoneToday: (Int, Int) -> String = { done, total -> "$done of $total done" }
    open val todayProgress: (Int, Int) -> String = { count, target -> "$count / $target today" }
    open val streakDays: (Int) -> String = { if (it == 1) "1 day streak" else "$it day streak" }
    open val timesDoneTotal: (Int) -> String = { "$it times in total" }
    open val goalReached: String = "Daily goal reached"
    open val timesToGo: (Int) -> String = { if (it == 1) "1 more to go" else "$it more to go" }
    open val statCurrentStreak: String = "Current streak"
    open val statBestStreak: String = "Best streak"
    open val statTotalTimes: String = "Times done"
    open val statDaysCompleted: String = "Days completed"
    open val last7Days: String = "Last 7 days"
    open val last30Days: String = "Last 30 days"
    open val calendarTitle: String = "Calendar"
    open val legendDone: String = "Done"
    open val legendPartial: String = "Partly"
    open val legendMissed: String = "Missed"
    open val editPastHint: String = "Forgot to log a day? Adjust it here."

    // Weekly good habits ("N days a week", Sunday to Saturday)
    open val habitFrequency: String = "How often"
    open val frequencyDaily: String = "Every day"
    open val frequencyWeekly: String = "Days a week"
    open val weeklyTarget: String = "Days a week"
    open val notDoneYet: String = "Not done yet"
    open val weeklyTargetHint: String = "Any days from Sunday to Saturday. Each day counts once."
    open val weekProgress: (Int, Int) -> String = { done, goal -> "$done / $goal days this week" }
    open val weekGoalReached: String = "Weekly goal reached"
    open val daysToGoThisWeek: (Int) -> String = { if (it == 1) "1 more day this week" else "$it more days this week" }
    open val thisWeekLabel: String = "This week"
    open val last4Weeks: String = "Last 4 weeks"
    open val weeksCount: (Int) -> String = { if (it == 1) "1 week" else "$it weeks" }
    open val streakWeeks: (Int) -> String = { if (it == 1) "1 week streak" else "$it week streak" }
    open val daysDoneTotal: (Int) -> String = { if (it == 1) "1 day in total" else "$it days in total" }
    open val statDaysDone: String = "Days done"
    open val statWeeksCompleted: String = "Weeks completed"
    open val infoCurrentStreakWeekly: String = "Weeks in a row you reached this habit's weekly goal. This week counts as soon as you reach it, and a missed week starts the count again."
    open val infoBestStreakWeekly: String = "The most weeks in a row you have ever reached the weekly goal."
    open val infoDaysDoneWeekly: String = "Every day you did this habit, all weeks added together. A day counts once."
    open val infoWeeksCompleted: String = "How many weeks you reached the full weekly goal."
    open val disciplineWaiting: String = "Your weekly habits count once this week's goal is met."

    // Events
    open val eventsEyebrow: String = "Look forward"
    open val eventsTitle: String = "Important events"
    open val eventsSubtitle: String = "Countdowns to what matters."
    open val eventsEmptyTitle: String = "No events yet"
    open val eventsEmptyBody: String = "Add a trip, a deadline or a celebration and watch the countdown."
    open val eventsAdd: String = "Add an event"
    open val upcoming: String = "Upcoming"
    open val pastEvents: String = "Past events"
    open val startsIn: String = "Starts in"
    open val passedSince: String = "Passed since"
    open val newEvent: String = "New event"
    open val editEvent: String = "Edit event"
    open val eventNameLabel: String = "Event name"
    open val eventNamePlaceholder: String = "e.g. Trip to Siwa"
    open val eventCategoryLabel: String = "Category"
    open val eventWhenLabel: String = "Date & time"
    open val eventPastWarning: String = "This moment has already passed — reminders won't be sent."
    open val remindMe: String = "Remind me"
    open val customReminder: String = "Custom reminder"
    open val reminderHint: String = "Pick as many reminders as you like. They arrive as notifications on this device."
    open val addEvent: String = "Add event"
    open val deleteEvent: String = "Delete event"
    open val atEventTime: String = "At event time"

    // --- Event reminders: list, edit, remove, undo, permissions (Agent 1) ---
    open val remindersTitle: String = "Reminders"
    open val remindersEmpty: String = "No reminders yet. Add one to get a notification before this moment."
    open val addReminder: String = "Add reminder"
    open val editReminder: String = "Edit reminder"
    open val removeReminder: String = "Remove reminder"
    open val quickAdd: String = "Quick add"
    open val reminderFiresAt: (String) -> String = { "Notifies you on $it" }
    open val reminderPassed: String = "Already passed, it will not notify you"
    open val reminderRemoved: String = "Reminder removed"
    open val undo: String = "Undo"
    open val reminderUnitLabel: String = "How long before?"
    open val reminderDialogSave: String = "Save reminder"
    open val reminderNotificationsOff: String = "Notifications are off, so reminders cannot reach you."
    open val reminderOpenSettings: String = "Open settings"
    open val reminderExactOff: String = "Exact alarms are off. Reminders still work but can arrive a few minutes late."
    open val reminderAllowExact: String = "Allow exact alarms"
    open val reminderSaveFailed: String = "Could not update the reminder. Please try again."
    open val reminderError: (com.adhamelgendy.nixura.domain.notification.ReminderError) -> String = {
        when (it) {
            com.adhamelgendy.nixura.domain.notification.ReminderError.NEGATIVE -> "Choose a time before the event."
            com.adhamelgendy.nixura.domain.notification.ReminderError.TOO_FAR_AHEAD -> "Reminders can be at most 365 days before the event."
            com.adhamelgendy.nixura.domain.notification.ReminderError.EVENT_PASSED -> "This event has already passed."
            com.adhamelgendy.nixura.domain.notification.ReminderError.IN_PAST -> "That reminder time has already passed. Choose a shorter lead time."
            com.adhamelgendy.nixura.domain.notification.ReminderError.DUPLICATE -> "You already have this reminder."
            com.adhamelgendy.nixura.domain.notification.ReminderError.TOO_MANY -> "An event can have at most 10 reminders."
        }
    }
    open val minutesBefore: (Int) -> String = { "$it min before" }
    open val hoursBefore: (Int) -> String = { if (it == 1) "1 hour before" else "$it hours before" }
    open val daysBefore: (Int) -> String = { if (it == 1) "1 day before" else "$it days before" }
    open val categoryLabel: (EventCategory) -> String = {
        when (it) {
            EventCategory.PERSONAL -> "Personal"
            EventCategory.TRAVEL -> "Travel"
            EventCategory.WORK -> "Work"
            EventCategory.HEALTH -> "Health"
            EventCategory.CELEBRATION -> "Celebration"
            EventCategory.OTHER -> "Other"
            EventCategory.EVENT -> "Event"
            EventCategory.BIRTHDAY -> "Birthday"
            EventCategory.TASK -> "Task"
            EventCategory.HOLIDAY -> "Holiday"
        }
    }

    // Coach
    open val coachEyebrow: String = "Your coach"
    open val coachTitle: String = "AI Coach"
    open val coachInputHint: String = "Ask for help, a plan or motivation…"
    open val coachOfflineNote: String = "Replies are generated on your device. Private, but not a substitute for professional care."
    open val coachModeOfflineShort: String = "Offline · private, on your device"
    open val coachPickHabit: String = "Choose a habit to talk about"

    // ---- Agent 4: what happens if I stop for ... ----
    open val coachQuickWhatHappens: String = "What happens if I stop for…"
    open val coachWhatHappensPrefix: String = "What happens if I stop for "
    open val stopExplorerTitle: String = "What happens if I stop for…"
    open val stopExplorerHint: String = "Pick a time or type your own. Timelines vary from person to person."
    open val stopExplorerNow: String = "My streak"
    open val stopExplorerChipLabels: List<String> = listOf("1h", "3h", "6h", "12h", "1d", "3d", "1w", "2w", "1mo")
    open val stopExplorerCustomPlaceholder: String = "e.g. 6 hours, 2 days, 3 weeks"
    open val stopExplorerShow: String = "Show"
    open val stopExplorerUnreadable: String = "I couldn't read that time. Try \"6 hours\" or \"2 days\" (up to 10 years)."
    open val stopExplorerHappeningLabel: String = "What is happening"
    open val stopExplorerNextLabel: String = "Coming next"
    open val stopExplorerNextIn: (String, String) -> String = { title, time -> "$title, about $time later" }
    open val stopExplorerNothingNext: String = "The main milestones on this timeline are behind you."
    open val stopExplorerTipsLabel: String = "If urges show up"
    open val stopExplorerSafetyLabel: String = "Important"

    // Profile
    open val profileTitle: String = "Profile"
    open val profileNoName: String = "Add your name"
    open val editProfile: String = "Edit profile"
    open val nameLabel: String = "Name"
    open val namePlaceholder: String = "What should we call you?"
    open val emailLabel: String = "Email"
    open val ageLabel: String = "Age"
    open val ageYears: (Int) -> String = { "$it years old" }
    open val yourProgress: String = "Your progress"
    open val profileTotals: String = "Totals"
    open val languageHint: String = "More languages are on the way."
    open val guideReplayHint: String = "A 30-second tour of the app."
    open val privacyAndData: String = "Privacy and data"
    open val quitScoreTitle: String = "Breaking free"
    open val quitScoreBody: String = "How far you are toward 30 days clear, on average"
    open val disciplineScoreTitle: String = "Discipline"
    open val disciplineScoreBody: String = "Your good-habit completion over the last 7 days"
    open val noBadHabitsYet: String = "Add a habit to quit to see this score."
    open val noGoodHabitsYet: String = "Add a good habit to see this score."
    open val statClearDays: String = "Clear days"
    open val statCheckIns: String = "Check-ins"
    open val statHabitsTracked: String = "Habits tracked"
    open val nearestEvent: String = "Nearest event"
    open val noUpcomingEvents: String = "No upcoming events. Tap to add one."

    // Settings
    open val settingsTitle: String = "Settings"
    open val appearance: String = "Appearance"
    open val darkMode: String = "Dark"
    open val lightMode: String = "Light"
    open val language: String = "Language"
    open val notifications: String = "Notifications"
    open val notificationsToggle: String = "Reminders & coaching"
    open val notificationsHint: String = "Event reminders, streak milestones and a daily check-in."
    open val dailyCheckIn: String = "Daily check-in"
    open val dailyCheckInHint: String = "A short note from your coach"
    open val dataTitle: String = "Your data"
    open val dataBody: String = "Delete all your good and bad habits, events and coach chats."
    open val eraseData: String = "Erase all data"
    open val eraseTitle: String = "Erase everything?"
    open val eraseBody: String = "All your good and bad habits, events and coach chats will be permanently deleted, also from your account and other devices if you're signed in. Your account and settings stay."
    open val eraseConfirm: String = "Erase"
    open val about: String = "About"
    open val versionLabel: String = "Version 1.0"
    open val aboutBody: String = "NixUra helps you break bad habits, build good ones and stay on track for the moments that matter."

    // Home-screen widgets (Android) and their chooser screen
    open val widgetQuitCaption: String = "since your last slip"
    open val widgetDaysSuffix: String = "d"
    open val widgetNoQuitHabits: String = "No quit habits yet"
    open val widgetNoGoodHabits: String = "No good habits yet"
    open val widgetNoEvents: String = "No events yet"
    open val widgetTapToAdd: String = "Tap to add one"
    open val widgetTapToChoose: String = "Tap to choose one"
    open val widgetTapToChooseAnother: String = "Tap to choose another"
    open val widgetHabitRemoved: String = "Habit removed"
    open val widgetEventRemoved: String = "Event removed"
    open val widgetGoodDone: String = "Done for today"
    open val widgetGoodToGo: String = "to go"
    open val widgetAddOne: String = "Add one"
    open val widgetRemoveOne: String = "Remove one"
    open val widgetEventNext: String = "Next event"
    open val widgetEventUntil: String = "until"
    open val widgetEventSince: String = "since"
    open val widgetError: String = "Couldn't load"
    open val widgetTapToOpen: String = "Tap to open NixUra"
    open val widgetConfigTitleQuit: String = "Choose a quit habit"
    open val widgetConfigTitleGood: String = "Choose a good habit"
    open val widgetConfigTitleEvent: String = "Choose an event"
    open val widgetConfigNearestEvent: String = "Nearest upcoming event"
    open val widgetConfigEmptyQuit: String = "You have no quit habits yet. Add one in NixUra first, or place the widget now and tap it later."
    open val widgetConfigEmptyGood: String = "You have no good habits yet. Add one in NixUra first, or place the widget now and tap it later."
    open val widgetConfigPlaceAnyway: String = "Place widget anyway"
    open val widgetConfigCancel: String = "Cancel"
    open val widgetAddToHome: String = "Add to Home screen"
    open val widgetAddToHomeAsk: String = "Confirm in the dialog, then place the widget where you want it. You can resize and move it afterwards."
    open val widgetAddToHomeManual: String = "Your launcher can't add widgets from here. Long-press the Home screen, open Widgets and pick NixUra."
    open val widgetAddToHomeFailed: String = "Couldn't add the widget. Try long-pressing the Home screen and choosing NixUra under Widgets."
    open val widgetTitleQuit: String = "Quit"
    open val widgetTitleBuild: String = "Build"

    // ---- Accounts (Agent 2) ----
    open val landingTagline: String = "Let's Nix the bad"
    open val landingAuraWord: String = "Aura"
    open val landingBody: String = "Quit what drains you. Build what lifts you. Count down to what matters."
    open val landingGetStarted: String = "Get Started"
    open val authCreateAccount: String = "Create account"
    open val authSignIn: String = "Sign in"
    open val authContinueOffline: String = "Continue as guest"
    open val authOfflineWarnTitle: String = "Continue as a guest?"
    open val authOfflineWarnBody: String = "As a guest the app works completely offline. Your data won't be saved to the cloud and can be lost if you uninstall the app, clear its data or lose your phone. You can sign in any time from your picture at the top of the app."
    open val authOfflineWarnConfirm: String = "Continue"
    open val authLeftoverTitle: String = "Your last session is still here"
    open val authLeftoverBody: String = "You were signed out, but that account's data is still on this phone. Sign in to keep it. A guest starts empty, so continuing removes it from this phone, including changes that were not uploaded yet."
    open val authLeftoverStartFresh: String = "Start empty"
    open val accountGuestName: String = "Guest"
    open val accountGuestBody: String = "Your data stays on this phone only"
    open val authRegisterTitle: String = "Create your account"
    open val authRegisterSubtitle: String = "We'll email you a link to confirm it's you."
    open val authEmailLabel: String = "Email"
    open val authPasswordLabel: String = "Password"
    open val authNewPasswordLabel: String = "New password"
    open val authCurrentPasswordLabel: String = "Current password"
    open val authPasswordHint: String = "At least 10 characters. A few random words work great."
    open val authShowPassword: String = "Show password"
    open val authHidePassword: String = "Hide password"
    open val authAgreeText: String = "By creating an account you agree to the"
    open val authTermsLink: String = "Terms of Use"
    open val authPrivacyLink: String = "Privacy Policy"
    open val authAnd: String = "and"
    open val authLoginTitle: String = "Welcome back"
    open val authLoginSubtitle: String = "Sign in to pick up where you left off."
    open val authForgotLink: String = "Forgot password?"
    open val authNewHere: String = "New here?"
    open val authHaveAccount: String = "Already have an account?"
    open val authVerifyTitle: String = "Check your email"
    open fun authVerifySubtitle(email: String): String = "We sent a link to $email. Tap it, then come back here: this screen moves on by itself."
    open val authVerifyButton: String = "I've confirmed it"
    open val authVerifyCheckSpam: String = "Nothing there? Check your spam folder."
    open val authResend: String = "Resend email"
    open fun authResendIn(seconds: Int): String = "Resend email in ${seconds}s"
    open val authLinkResent: String = "If that address can receive mail, a new link is on its way."
    open val authForgotTitle: String = "Reset your password"
    open val authForgotSubtitle: String = "Enter your email and we'll send you a link to choose a new password."
    open val authSendLink: String = "Send link"
    open val authForgotSentTitle: String = "Check your email"
    open fun authForgotSent(email: String): String = "If $email has an account, a link to choose a new password is on its way. Open it, set the new password, then sign in here."
    open val authOrDivider: String = "or"
    open val authContinueWithGoogle: String = "Continue with Google"
    open val authContinueWithFacebook: String = "Continue with Facebook"
    open val authBack: String = "Back"
    open val authStrengthWeak: String = "Weak"
    open val authStrengthFair: String = "Fair"
    open val authStrengthGood: String = "Good"
    open val authStrengthStrong: String = "Strong"
    open fun authStrengthLabel(level: String): String = "Password strength: $level"
    open val authEmailEmpty: String = "Enter your email."
    open val authEmailInvalid: String = "Enter a valid email address."
    open val authCodeIncomplete: String = "Enter all 6 digits."
    open val authPasswordRequired: String = "Enter your password."
    open val pwTooShort: String = "Use at least 10 characters."
    open val pwTooLong: String = "Use at most 128 characters."
    open val pwTooCommon: String = "That password is too common."
    open val pwSameAsEmail: String = "Don't use your email as your password."
    open val pwLowVariety: String = "Use a wider mix of characters."
    open val pwBreached: String = "This password appeared in a data breach. Choose another."
    open val pwSameAsCurrent: String = "Choose a password different from the current one."
    open val errInvalidCredentials: String = "Email or password is incorrect."
    open val errNotVerified: String = "Your email isn't confirmed yet. Tap the link we sent, then try again."
    open val errInvalidCode: String = "That link is wrong or has expired. Request a new one."
    open val errNotAvailable: String = "This way of signing in isn't available on this phone yet."
    open val errAccountExistsOther: String = "This email already has a NixUra account that signs in another way. Sign in that way instead."
    open val errEmailInUse: String = "This email already has a NixUra account. Sign in instead, with your password or with Google or Facebook if you used one."
    open val errRecentLogin: String = "For your safety, sign out, sign back in, then try again."
    open val usageStatsToggle: String = "Share anonymous usage stats"
    open val usageStatsHint: String = "Things like how often the app is opened, to help improve NixUra. Never your habits, notes or chats. Off unless you turn it on."
    open fun errLocked(seconds: Int?): String =
        if (seconds == null) "Too many attempts. Try again later." else "Too many attempts. Try again in ${(seconds + 59) / 60} min."
    open val errRateLimited: String = "Too many requests. Please wait a moment."
    open val errNetwork: String = "Can't reach NixUra. Check your connection and try again."
    open val errServer: String = "Something went wrong on our side. Try again soon."
    open val errSessionExpired: String = "Your session ended. Please sign in again."
    open val errInvalidInput: String = "Please check the fields and try again."
    open val errUnknown: String = "Something went wrong. Please try again."
    open val accountTitle: String = "Account"
    open val accountSignedInAs: String = "Signed in as"
    open val accountSignedOutBody: String = "You're using NixUra on this device only. Create an account to sign in anywhere."
    open val accountSignOut: String = "Sign out"
    open val accountSignOutHint: String = "Removes this account's data from this phone."
    open val accountSecurity: String = "Sign-in and security"
    open val accountChangePasswordHint: String = "Other phones will need the new password."
    open val accountLegal: String = "Legal"
    open val legalPrivacyHint: String = "How NixUra handles your data."
    open val legalTermsHint: String = "The rules for using NixUra."
    open val accountDangerZone: String = "Danger zone"
    open val accountDeleteHint: String = "Permanently deletes your account."
    open val accountSignOutTitle: String = "Sign out?"
    open val accountSignOutBody: String = "We'll save your latest changes to your account, then remove your data from this phone. It all comes back when you sign in again."
    open val accountSignOutUnsavedTitle: String = "Some changes aren't saved"
    open val accountSignOutUnsavedBody: String = "We couldn't reach the server, so your latest changes aren't in your account yet. If you sign out now, they will be lost. Try again when you're online."
    open val accountSignOutAnyway: String = "Sign out anyway"
    open val syncTitle: String = "Cloud sync"
    open val syncNow: String = "Sync now"
    open val syncInProgress: String = "Syncing..."
    open val syncNeverYet: String = "Not synced yet"
    open val syncJustNow: String = "Synced just now"
    open fun syncMinutesAgo(minutes: Long): String = "Synced $minutes min ago"
    open fun syncHoursAgo(hours: Long): String = "Synced $hours h ago"
    open fun syncDaysAgo(days: Long): String = "Synced $days d ago"
    open fun syncOffline(since: String): String = "Offline. $since"
    open fun syncFailed(since: String): String = "Couldn't sync. $since"
    open val accountChangePassword: String = "Change password"
    open val accountDelete: String = "Delete account"
    open val accountEmailManaged: String = "Your email belongs to your account and can't be edited here."
    open val settingsAccountRow: String = "Account, sign-in and privacy"
    open val cpTitle: String = "Change password"
    open val cpButton: String = "Update password"
    open val cpDone: String = "Password updated. Your other devices were signed out."
    open val delTitle: String = "Delete account"
    open val delWarn: String = "This permanently deletes your NixUra account: its email, name and sign-in. Habits stored on this phone are not removed. This can't be undone."
    open val delNoPasswordHint: String = "Your account signs in with Google or Facebook, so no password is needed to confirm."
    open val delPasswordLabel: String = "Enter your password to confirm"
    open val delButton: String = "Delete my account"
    open val delConfirmTitle: String = "Delete your account?"
    open val delConfirmBody: String = "This cannot be undone."
    open val legalPrivacyTitle: String = "Privacy Policy"
    open val legalTermsTitle: String = "Terms of Use"
    open val legalDraftNotice: String = "Draft: the owner must fill in the bracketed details and have this reviewed before release."

    // Score and streak explanations
    open val scoreHowTitle: String = "How it's worked out"
    open val scoreRuleBreakingFree: String = "Each quit habit counts up to 100% over 30 clear days. A relapse starts that habit again. This score is the average of all of them."
    open val scoreRuleDiscipline: String = "For each good habit: how much of its target you reached. A daily habit looks at the last 7 days. A weekly habit looks at this week once its goal is met (or can no longer be met), and at last week until then. This score is the average of all of them."
    open val scoreRuleMomentum: String = "The average of Breaking free and Discipline. If you only track one kind of habit, it is that score."
    open val scoreTipBreakingFree: String = "Every clear hour moves this up."
    open val scoreTipDiscipline: String = "Reach today's targets to lift it. Days before a habit existed don't count against you, and a weekly habit only lowers it once its goal can no longer be met."
    open val scoreTipMomentum: String = "Tap Breaking free or Discipline on the dashboard to see each habit's share."
    open val infoCurrentStreak: String = "Days in a row you reached this habit's daily target. Today counts as soon as you reach it, and a missed day starts the count again."
    open val infoBestStreak: String = "The most days in a row you have ever reached the daily target."
    open val infoTotalTimes: String = "Every time you have logged this habit, all days added together."
    open val infoDaysCompleted: String = "How many days you reached the full daily target."
    open val infoClearFor: String = "Time since your last relapse. It keeps counting by itself, and the card gets lighter as this grows: at 1, 3, 7, 14 and 30 days."
    open val infoBestQuit: String = "Your longest time clear on this habit. A relapse never erases it."
    open val infoRelapses: String = "How many relapses you recorded. They are part of your history and never lower your best."
    open val infoTrackingSince: String = "The day you started tracking this habit."
    open val infoClearDays: String = "Full days clear right now, added up across all your quit habits."
    open val infoBestAcross: String = "Your longest time clear on any quit habit."
    open val infoCheckIns: String = "Every time you logged a good habit, all added together."
    open val infoHabitsTracked: String = "Your quit habits and good habits together."
    open val infoBestActive: String = "The quit habit you've been clear on the longest right now."

    // Dashboard
    open val guestBannerTitle: String = "You're using NixUra as a guest"
    open val guestBannerBody: String = "Your data is only on this phone. Sign in to back it up."
    open val homeStartTitle: String = "Start with one thing"
    open val homeStartQuit: String = "Something to quit"
    open val homeStartBuild: String = "A habit to build"
    open val homeStartEvent: String = "An event to count down to"
    open val homeThought: String = "Today's thought"
    open val homeToday: String = "Today"

    // Custom habits
    open val customHabitPill: String = "+ Write my own"

    // Coach
    open val coachWelcomeSubtitle: String = "Your private coach. It works offline."
    open val coachStatBest: String = "Best right now"
    open val coachStatHabits: String = "Quitting"
    open val coachStatToday: String = "Done today"
    open val coachPickStart: String = "What do you need?"
    open val coachHistoryTitle: String = "Chats"
    open val coachHistoryEmpty: String = "Your past chats will show up here."
    open val coachNewChat: String = "New chat"
    open val coachUntitledChat: String = "New chat"
    open val coachDeleteChatTitle: String = "Delete this chat?"
    open val coachDeleteChatBody: String = "It will be removed from this phone. This cannot be undone."

    // Starter guide
    open val guideSkip: String = "Skip"
    open val guideNext: String = "Next"
    open val guideWelcomeTitle: String = "Welcome to NixUra"
    open val guideWelcomeBody: String = "Quit what drains you, build what lifts you. Here's a quick tour of the real app. It takes 30 seconds."
    open val guideStart: String = "Show me around"
    open val guideScoresTitle: String = "Your progress at a glance"
    open val guideScoresBody: String = "These rings fill up as you stay clear and reach your targets. Tap one any time to see how it's worked out."
    open val guideQuitTitle: String = "Quit a bad habit"
    open val guideQuitBody: String = "Tap + to add one. A live counter starts, and its card gets lighter the longer you stay clear."
    open val guideBuildTitle: String = "Build a good habit"
    open val guideBuildBody: String = "Add things like drinking water, then tap + during the day. The card fills with green until you're done."
    open val guideEventsTitle: String = "Count down to what matters"
    open val guideEventsBody: String = "Add an event, set reminders, and watch the time count down."
    open val guideBarTitle: String = "Get around"
    open val guideBarBody: String = "Tap here or swipe between screens. The last one is your coach, for cravings and hard moments."
    open val guideAccountTitle: String = "Your account"
    open val guideAccountBody: String = "Tap your picture to sign in, sign out, or open Profile and Settings."
    open val guideDoneTitle: String = "You're ready"
    open val guideDoneBody: String = "The best first step is one habit you want to leave behind."
    open val guideAddFirst: String = "Add my first habit"
    open val guideExplore: String = "Look around first"
    open val guideReplay: String = "Replay the starter guide"

    // Long-press delete with undo
    open val deleteAction: String = "Delete"
    open fun deleteItemTitle(name: String): String = "Delete “$name”?"
    open val deleteBadHabitBody: String = "Its streak and every relapse you logged are removed. If you're signed in, it goes from your other devices too."
    open val deleteGoodHabitBody: String = "Every day you logged is removed. If you're signed in, it goes from your other devices too."
    open val deleteEventItemBody: String = "The countdown and its reminders are removed."
    open fun deletedItem(name: String): String = "“$name” deleted"

    // Money-saved tracker
    open val moneyTitle: String = "Money saved"
    open val moneyOptIn: String = "Track the money you save"
    open val moneyOptInBody: String = "Optional. Tell us what it cost you and watch the savings grow while you stay clear."
    open val moneyWhatExactly: String = "What exactly?"
    open val moneyWhatCigarettes: String = "Brand, e.g. Cleopatra (optional)"
    open val moneyWhatOther: String = "Which one, e.g. Pepsi (optional)"
    open val moneyBoughtAs: String = "Bought as"
    open val moneyCountBy: String = "Count per day in"
    open val moneyPerDay: String = "How many a day, on average?"
    open val moneyPacksPerDay: String = "Packs a day"
    open val moneyPerPack: String = "How many in one pack?"
    open val moneyPrice: String = "Price"
    open val moneyCurrency: String = "Currency"
    open val moneyRemove: String = "Stop tracking money"
    open val moneyInvalid: String = "Enter how many a day and the price, or turn the tracker off."
    open val moneySavedThisStreak: String = "Saved this streak"
    open val moneyDailyCost: String = "A normal day cost"
    open val moneyPerStreak: String = "Saved in each clean streak"
    open val moneyNow: String = "Now"
    open val moneyEdit: String = "Edit costs"
    open val moneyAddCta: String = "Add what it cost you to see the money you save."
    open fun moneyPricePer(unit: String): String = "Price per $unit"
    open fun moneyDailyCostLine(amount: String, perDay: String): String = "$amount a day ($perDay)"
    open fun moneyCardLine(amount: String, avoided: String): String = "$amount saved · $avoided"

    open fun costUnitName(unit: CostUnit, plural: Boolean): String = when (unit) {
        CostUnit.CIGARETTE -> if (plural) "cigarettes" else "cigarette"
        CostUnit.POD -> if (plural) "pods" else "pod"
        CostUnit.POUCH -> if (plural) "pouches" else "pouch"
        CostUnit.SESSION -> if (plural) "sessions" else "session"
        CostUnit.CUP -> if (plural) "cups" else "cup"
        CostUnit.CAN -> if (plural) "cans" else "can"
        CostUnit.BOTTLE -> if (plural) "bottles" else "bottle"
        CostUnit.DRINK -> if (plural) "drinks" else "drink"
        CostUnit.MEAL -> if (plural) "meals" else "meal"
    }

    /** "340 cigarettes not smoked", "21 cans not drunk". */
    open fun unitsAvoided(count: Long, unit: CostUnit): String {
        val verb = when (unit) {
            CostUnit.CIGARETTE -> "not smoked"
            CostUnit.CUP, CostUnit.CAN, CostUnit.BOTTLE, CostUnit.DRINK -> "not drunk"
            CostUnit.MEAL -> "not eaten"
            CostUnit.SESSION -> "skipped"
            CostUnit.POD, CostUnit.POUCH -> "not used"
        }
        return "$count ${costUnitName(unit, count != 1L)} $verb"
    }
    open val packWord: String = "pack"

    // Integrations (Google Calendar, Notion)
    open val integrationsTitle: String = "Integrations"
    open val integrationsRow: String = "Calendars"
    open val integrationsRowHint: String = "Import events from Google Calendar or Notion"
    open val integrationsIntro: String = "Linked events appear in Events with their countdown, and stay up to date. NixUra only reads them."
    open val integrationsNeedAccount: String = "Sign in to link a calendar. Guests use NixUra completely offline."
    open val googleCalendar: String = "Google Calendar"
    open val notion: String = "Notion"
    open val integrationConnect: String = "Connect"
    open val integrationReconnect: String = "Connect again"
    open val integrationDisconnect: String = "Disconnect"
    open val integrationSyncNow: String = "Sync now"
    open val integrationNotConnected: String = "Not connected"
    open fun integrationConnectedAs(label: String): String = if (label.isBlank()) "Connected" else "Connected · $label"
    open val integrationNeverSynced: String = "Not synced yet"
    open fun integrationSyncedAgo(ago: String): String = "Last synced $ago"
    open val integrationErrRevoked: String = "Access was removed or expired. Connect again to keep importing."
    open val integrationErrOffline: String = "You're offline. NixUra will try again later."
    open val integrationErrNotConfigured: String = "Pick what to import below."
    open val integrationErrFailed: String = "Couldn't sync this time. NixUra will try again later."
    open val googleNotAvailable: String = "Google Calendar isn't set up in this build yet."
    open val googleCancelled: String = "Google Calendar wasn't connected. Tap Connect to try again."
    open val googleDenied: String = "Google access wasn't granted."
    open val notionNotConnected: String = "Finish connecting Notion in your browser, then come back."
    open val googlePickCalendars: String = "What to import"
    open val googlePickHint: String = "Highlighted items are imported. Tap one to turn it on or off."
    open val googleFeedEvents: String = "Events"
    open val googleFeedBirthdays: String = "Birthdays"
    open val googleFeedTasks: String = "Tasks"
    open val googleFeedHolidays: String = "Holidays"
    open val notionPickDatabase: String = "Database to import"
    open val notionNoDatabases: String = "No databases shared yet. In Notion's connect page, share the database that holds your events."
    open val notionTitleProperty: String = "Event name from"
    open val notionDateProperty: String = "Event date from"
    open val notionNoDateProperty: String = "This database has no date property, so it can't be imported."
    open fun disconnectTitle(name: String): String = "Disconnect $name?"
    open val disconnectBody: String = "Events imported from it are removed from NixUra (on your other devices too). Nothing changes in the calendar itself."
    open fun importedFrom(name: String): String = "Imported from $name. Edit it there; your reminders here are kept."
    open val sourceGoogle: String = "Google"
    open val sourceNotion: String = "Notion"
    open val agoJustNow: String = "just now"
    open fun agoMinutes(n: Long): String = "$n min ago"
    open fun agoHours(n: Long): String = "$n h ago"
    open fun agoDays(n: Long): String = "$n d ago"

    // --- AI coach, progress cards and voice notes
    open val aiConsentTitle: String = "Turn on the AI coach?"
    open val aiConsentPoints: List<String> = listOf(
        "Your messages, voice notes, habit names with their numbers, and event titles and dates are sent to online AI services to write the reply.",
        "Your name, email and account are never sent. Voice recordings are not kept on your phone.",
        "AI can be wrong, and it is not a doctor. You can switch it off in Settings at any time; the offline coach always works.",
    )
    open val aiConsentAgree: String = "Turn on AI coach"
    open val aiConsentDecline: String = "Keep the offline coach"
    open val aiSettingTitle: String = "AI coach"
    open val aiSettingHint: String = "Real AI replies and voice notes. Sends your messages and habit data to online AI services. Needs an account."
    open val coachModeAi: String = "AI coach · online"
    open fun coachModeAiLeft(left: Int): String = if (left == 1) "AI coach · 1 message left today" else "AI coach · $left messages left today"
    open val coachModeGuest: String = "Offline coach · sign in for the AI coach"
    open val coachNoticeLimit: String = "You've used today's AI messages. The offline coach answers until tomorrow."
    open val coachNoticeResting: String = "The AI coach is resting right now, so the offline coach answered."
    open val coachNoticeOffline: String = "No internet, so the offline coach answered."
    open val voiceCardTitle: String = "Say it, I'll add it"
    open val voiceCardBody: String = "Tap and tell me the habits and events you want."
    open val voiceSignInTitle: String = "Voice notes need an account"
    open val voiceSignInBody: String = "Sign in to use the AI coach and add habits and events with your voice."
    open val voiceAllowMicTitle: String = "Allow the microphone"
    open val voiceAllowMicBody: String = "NixUra records only while this sheet is listening, and never keeps the recording."
    open val voiceAllowMic: String = "Allow microphone"
    open val voiceListening: String = "I'm listening…"
    open val voiceExample: String = "Try: “I want to stop smoking, drink 5 cups of water a day, and I have a meeting next Monday at 7 PM.”"
    open val voiceTapToStop: String = "Tap the button when you're done"
    open val voiceUnderstanding: String = "Understanding…"
    open val voiceUnderstandingHint: String = "Working out what to add. This takes a few seconds."
    open val voiceReviewTitle: String = "Here's what I understood"
    open val voiceSuggestTitle: String = "Add these for you?"
    open val voiceReviewHint: String = "Tap an item to leave it out, or the pencil to fix it. Nothing is added until you confirm."
    open val voiceEditItem: String = "Fix this item"
    open val voiceEditTitle: String = "Fix what I got wrong"
    open val voiceEditHint: String = "Change anything I misheard. Nothing is added until you confirm the list."
    open val voiceEditKind: String = "Type"
    open val voiceEditKeep: String = "Keep changes"
    open val voiceNeedsFix: String = "Needs a fix before it can be added"
    open val voiceProblemName: String = "Give it a name."
    open fun voiceProblemNameTooLong(max: Int): String = "Keep the name to $max characters or fewer."
    open val voiceProblemDailyTarget: String = "Pick 1 to 99 times a day."
    open val voiceProblemDaysAWeek: String = "Pick 1 to 7 days a week."
    open val voiceProblemBadDate: String = "That date doesn't exist. Pick another one."
    open val voiceProblemPast: String = "This moment has already passed. Pick a time in the future."
    open fun voiceAdd(count: Int): String = if (count == 1) "Add 1 item" else "Add $count items"
    open fun voiceDoneTitle(added: Int): String = if (added == 1) "1 item added" else "$added items added"
    open val voiceDoneBody: String = "Counters and countdowns have started."
    open fun voiceDoneFailed(failed: Int): String = if (failed == 1) "1 item couldn't be added." else "$failed items couldn't be added."
    open val voiceRecordAnother: String = "Record another"
    open val voiceTryAgain: String = "Try again"
    open val voiceFailTitle: String = "That didn't work"
    open val voiceFailMic: String = "The microphone couldn't be opened. Close other apps that use it and try again."
    open val voiceFailNothingHeard: String = "I couldn't hear anything. Hold the phone a little closer and try again."
    open val voiceFailNothingToAdd: String = "I heard you, but found no habit or event to add. Try saying what you want to stop, start or attend."
    open val voiceFailLimit: String = "You've used today's voice notes. They come back tomorrow."
    open val voiceFailResting: String = "The AI is resting right now. Try again in a little while."
    open val voiceFailOffline: String = "No internet connection. Voice notes need to be online."
    open val actionQuit: String = "Quit"
    open val actionQuitDetail: String = "The counter starts now"
    open val actionBuild: String = "Build"
    open fun actionTimesADay(count: Int): String = if (count == 1) "Every day" else "$count times a day"
    open fun actionDaysAWeek(count: Int): String = if (count == 1) "1 day a week" else "$count days a week"
    open val actionEvent: String = "Event"
    open val visualQuitEyebrow: String = "Quitting"
    open val visualGoodEyebrow: String = "Building"
    open fun visualToGoal(days: Int): String = if (days == 1) "to 1 day" else "to $days days"
    open val visualClean: String = "clean and counting"
    open fun visualBest(time: String): String = "Best streak: $time"
    open fun visualRelapses(count: Int): String = if (count == 1) "1 relapse" else "$count relapses"
    open val visualToday: String = "today"
    open val visualThisWeek: String = "this week"
    open fun visualStreakDays(days: Int): String = if (days == 1) "1 day streak · last 7 days" else "$days day streak · last 7 days"
    open fun visualStreakWeeks(weeks: Int): String = if (weeks == 1) "1 week streak · last 7 days" else "$weeks week streak · last 7 days"
    open val visualOverviewTitle: String = "Your progress"
    open val visualWeek: String = "this week"
    open val visualLongestClean: String = "Longest clean streak"
    open val visualQuitting: String = "Habits you're quitting"
    open val visualDoneToday: String = "Good habits done today"
}

data class AppLanguage(val code: String, val nativeName: String)

val availableLanguages: List<AppLanguage> = listOf(
    AppLanguage("en", "English"),
)

val EnglishStrings: Strings = Strings()

fun stringsFor(code: String): Strings = when (code) {
    else -> EnglishStrings
}

val LocalStrings = staticCompositionLocalOf { EnglishStrings }
