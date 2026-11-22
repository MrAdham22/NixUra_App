package com.adhamelgendy.nixura.notifications

import android.app.Notification
import android.content.Context
import androidx.annotation.RequiresApi

/**
 * Android 17's native metric template for "Today": up to three values ("3/8", "2/5", next "15:12"). The countdown
 * to next stays in the header, where the system ticks it (as a metric it was cut to "1h 22…" on a Pixel 9).
 * NotificationCompat has no version of it, so the built notification gets the platform style. Never asks to be
 * promoted to a Live Update (Google lists an all-day summary as not allowed there).
 */
@RequiresApi(37)
internal object TodayMetricStyle {
    private const val MAX_METRICS = 3

    fun applyTo(context: Context, built: Notification, request: NixNotificationRequest): Notification {
        val next = request.countdownClock
        val room = if (next != null) MAX_METRICS - 1 else MAX_METRICS
        val style = Notification.MetricStyle()
        request.metrics.take(room).forEach {
            style.addMetric(Notification.Metric(Notification.Metric.FixedText(it.value), it.label))
        }
        if (next != null) {
            // Text with a unit, not FixedTime: that one prints "8:50 PM" as one value and is cut to "8:50 …".
            val value = Notification.Metric.FixedText(next.time, next.suffix)
            style.addMetric(Notification.Metric(value, request.countdownLabel.orEmpty()))
        }
        return Notification.Builder.recoverBuilder(context, built).setStyle(style).build()
    }
}
