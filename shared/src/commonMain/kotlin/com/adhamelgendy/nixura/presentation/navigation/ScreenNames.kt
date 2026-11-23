package com.adhamelgendy.nixura.presentation.navigation

/**
 * The fixed name a screen has in usage stats. Never includes what a route carries (habit or event ids, the email on
 * the verify screen): only which kind of screen it is. A form is "…_new" or "…_edit".
 */
fun screenNameOf(route: Route): String = when (route) {
    Route.Home -> "home"
    Route.BadHabits -> "quit_habits"
    Route.GoodHabits -> "good_habits"
    Route.Events -> "events"
    Route.Coach -> "coach"
    Route.CoachHistory -> "coach_history"
    Route.Profile -> "profile"
    Route.Settings -> "settings"
    Route.Integrations -> "calendars"
    is Route.BadHabitDetail -> "quit_habit_detail"
    is Route.GoodHabitDetail -> "good_habit_detail"
    is Route.BadHabitForm -> if (route.editId == null) "quit_habit_new" else "quit_habit_edit"
    is Route.GoodHabitForm -> if (route.editId == null) "good_habit_new" else "good_habit_edit"
    is Route.EventForm -> if (route.editId == null) "event_new" else "event_edit"
    Route.Auth.Welcome -> "welcome"
    Route.Auth.Login -> "sign_in"
    Route.Auth.Register -> "create_account"
    is Route.Auth.VerifyEmail -> "verify_email"
    Route.Auth.ForgotPassword -> "forgot_password"
    Route.Account -> "account"
    Route.ChangePassword -> "change_password"
    Route.DeleteAccount -> "delete_account"
    is Route.Legal -> "legal_" + route.document.name.lowercase()
    Route.KeepNixUra -> "keep_nixura"
    Route.ReportBug -> "report_bug"
    Route.Help -> "help"
}
