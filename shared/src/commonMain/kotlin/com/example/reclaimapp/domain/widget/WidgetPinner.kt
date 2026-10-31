package com.example.reclaimapp.domain.widget

/** Outcome of asking the launcher to place a widget. */
enum class WidgetPinResult {
    /** The launcher is showing its "add widget" dialog / placement mode. */
    REQUESTED,

    /** This device or launcher cannot pin widgets from an app (or the platform has no widgets). */
    UNSUPPORTED,

    /** The request could not be sent. */
    FAILED,
}

/**
 * Port for "Add to Home screen". Implemented on Android (`AppWidgetManager.requestPinAppWidget`); there is no
 * binding on iOS yet, and the UI hides the button when none is registered.
 */
interface WidgetPinner {
    fun isSupported(): Boolean

    /** Asks the launcher to place a [kind] widget already bound to [targetId] (habit/event id; `null` = nearest event). */
    fun requestPin(kind: WidgetKind, targetId: Long?): WidgetPinResult
}
