package com.example.reclaimapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.reclaimapp.domain.widget.WidgetKind
import com.example.reclaimapp.domain.widget.WidgetPinResult
import com.example.reclaimapp.domain.widget.WidgetPinner

/**
 * "Add to Home screen" on Android: `AppWidgetManager.requestPinAppWidget`. The launcher shows its own dialog and
 * placement UI (where the user chooses the page/position, and can resize or move the widget afterwards). The chosen
 * habit/event is remembered as a short-lived "pending pin" and applied when the new widget appears, so the chooser
 * screen is skipped (see [WidgetConfigActivity] and [WidgetPinReceiver]).
 */
class AndroidWidgetPinner(context: Context) : WidgetPinner {
    private val app = context.applicationContext

    override fun isSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            runCatching { AppWidgetManager.getInstance(app).isRequestPinAppWidgetSupported }.getOrDefault(false)

    override fun requestPin(kind: WidgetKind, targetId: Long?): WidgetPinResult {
        if (!isSupported()) return WidgetPinResult.UNSUPPORTED
        return try {
            WidgetPrefs.setPendingPin(app, kind, targetId)
            val callback = Intent(app, WidgetPinReceiver::class.java).apply {
                action = WidgetIntents.ACTION_PINNED
                data = Uri.parse("nixura://widget/pinned/${kind.name}/${targetId ?: 0}")
                putExtra(WidgetIntents.EXTRA_KIND, kind.name)
                putExtra(WidgetIntents.EXTRA_ID, targetId ?: -1L)
            }
            // FLAG_MUTABLE: the system adds EXTRA_APPWIDGET_ID to this (explicit, our own) intent.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val pending = PendingIntent.getBroadcast(app, kind.ordinal * 31 + (targetId ?: 0L).hashCode(), callback, flags)
            val sent = AppWidgetManager.getInstance(app).requestPinAppWidget(WidgetKinds.providerOf(app, kind), null, pending)
            if (sent) WidgetPinResult.REQUESTED else WidgetPinResult.FAILED
        } catch (e: Exception) {
            Log.w("NixUraWidgets", "pin request failed", e)
            WidgetPinResult.FAILED
        }
    }
}
