package com.example.reclaimapp.domain.widget

/**
 * All text shown by the Android home-screen widgets and their chooser screen (English defaults).
 *
 * Kept in its own class, next to the widget logic, so the widgets compile on their own and never depend on the
 * shared `Strings` class (which several people edit at once). Add a language by returning another instance in
 * [forLanguage].
 */
data class WidgetStrings(
    val widgetQuitCaption: String = "since your last slip",
    val widgetDaysSuffix: String = "d",
    val widgetNoQuitHabits: String = "No quit habits yet",
    val widgetNoGoodHabits: String = "No good habits yet",
    val widgetNoEvents: String = "No events yet",
    val widgetTapToAdd: String = "Tap to add one",
    val widgetTapToChoose: String = "Tap to choose one",
    val widgetTapToChooseAnother: String = "Tap to choose another",
    val widgetHabitRemoved: String = "Habit removed",
    val widgetEventRemoved: String = "Event removed",
    val widgetGoodDone: String = "Done for today",
    val widgetGoodToGo: String = "to go",
    val widgetAddOne: String = "Add one",
    val widgetRemoveOne: String = "Remove one",
    val widgetEventNext: String = "Next event",
    val widgetEventUntil: String = "until",
    val widgetEventSince: String = "since",
    val widgetError: String = "Couldn't load",
    val widgetTapToOpen: String = "Tap to open NixUra",
    val widgetConfigTitleQuit: String = "Choose a quit habit",
    val widgetConfigTitleGood: String = "Choose a good habit",
    val widgetConfigTitleEvent: String = "Choose an event",
    val widgetConfigNearestEvent: String = "Nearest upcoming event",
    val widgetConfigEmptyQuit: String = "You have no quit habits yet. Add one in NixUra first, or place the widget now and tap it later.",
    val widgetConfigEmptyGood: String = "You have no good habits yet. Add one in NixUra first, or place the widget now and tap it later.",
    val widgetConfigPlaceAnyway: String = "Place widget anyway",
    val widgetConfigCancel: String = "Cancel",
    val widgetAddToHome: String = "Add to Home screen",
    val widgetAddToHomeAsk: String = "Confirm in the dialog, then place the widget where you want it. You can resize and move it afterwards.",
    val widgetAddToHomeManual: String = "Your launcher can't add widgets from here. Long-press the Home screen, open Widgets and pick NixUra.",
    val widgetAddToHomeFailed: String = "Couldn't add the widget. Try long-pressing the Home screen and choosing NixUra under Widgets.",
    val widgetTitleQuit: String = "Quit",
    val widgetTitleBuild: String = "Build",
) {
    companion object {
        fun forLanguage(@Suppress("UNUSED_PARAMETER") code: String): WidgetStrings = WidgetStrings()
    }
}
