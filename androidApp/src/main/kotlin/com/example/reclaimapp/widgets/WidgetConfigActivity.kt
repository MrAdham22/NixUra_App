package com.example.reclaimapp.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.example.reclaimapp.domain.repository.EventRepository
import com.example.reclaimapp.domain.repository.GoodHabitRepository
import com.example.reclaimapp.domain.repository.HabitTrackingRepository
import com.example.reclaimapp.domain.usecase.LoadWidgetStateUseCase
import com.example.reclaimapp.domain.widget.WidgetKind
import com.example.reclaimapp.domain.widget.WidgetStrings
import kotlinx.coroutines.flow.first
import org.koin.mp.KoinPlatform
import java.text.DateFormat
import java.util.Date

/**
 * Shown by the launcher when a widget is placed (and again from "Tap to choose another" / reconfigure).
 * Plain Views on purpose: no extra dependencies, and it works before the Compose UI has ever run.
 */
class WidgetConfigActivity : Activity() {

    private class Option(val label: String, val targetId: Long?)

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var kind: WidgetKind

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Until the user picks something, the result is "cancelled" so the launcher drops a half-added widget.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        val resolved = if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) null else WidgetKinds.of(this, widgetId)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID || resolved == null) {
            finish()
            return
        }
        kind = resolved
        // Came from "Add to Home screen" on a habit/event screen: already knows what to show, skip the chooser.
        WidgetPrefs.takePendingPin(this, kind)?.let { pin ->
            save(pin.targetId)
            return
        }
        setContentView(root(loading = true, title = "", body = null))
        WidgetUpdater.launch {
            val (strings, options) = try {
                load()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                WidgetStrings() to emptyList()
            }
            runOnUiThread { if (!isFinishing) show(strings, options) }
        }
    }

    private suspend fun load(): Pair<WidgetStrings, List<Option>> {
        val koin = KoinPlatform.getKoin()
        val strings = WidgetStrings.forLanguage(koin.get<LoadWidgetStateUseCase>().appearance().languageCode)
        val options = when (kind) {
            WidgetKind.QUIT_COUNTER -> koin.get<HabitTrackingRepository>().observeActiveHabitTracking().first()
                .filter { !it.habit.isArchived }
                .map { Option(it.habit.name, it.habit.id) }
            WidgetKind.GOOD_QUICK_LOG -> koin.get<GoodHabitRepository>().observeGoodHabits().first()
                .map { Option(it.habit.name, it.habit.id) }
            WidgetKind.EVENT -> {
                val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                listOf(Option(strings.widgetConfigNearestEvent, null)) +
                    koin.get<EventRepository>().observeEvents().first()
                        .sortedBy { it.targetAtEpochMillis }
                        .map { Option("${it.title}  ·  ${format.format(Date(it.targetAtEpochMillis))}", it.id) }
            }
        }
        return strings to options
    }

    private fun show(strings: WidgetStrings, options: List<Option>) {
        val title = when (kind) {
            WidgetKind.QUIT_COUNTER -> strings.widgetConfigTitleQuit
            WidgetKind.GOOD_QUICK_LOG -> strings.widgetConfigTitleGood
            WidgetKind.EVENT -> strings.widgetConfigTitleEvent
        }
        if (options.isEmpty()) {
            val message = if (kind == WidgetKind.QUIT_COUNTER) strings.widgetConfigEmptyQuit else strings.widgetConfigEmptyGood
            val root = root(loading = false, title = title, body = message)
            root.addView(button(strings.widgetConfigPlaceAnyway) { save(null) })
            root.addView(button(strings.widgetConfigCancel) { finish() })
            setContentView(root)
            return
        }
        val root = root(loading = false, title = title, body = null)
        val list = ListView(this).apply {
            adapter = ArrayAdapter(this@WidgetConfigActivity, android.R.layout.simple_list_item_1, options.map { it.label })
            setOnItemClickListener { _, _, position, _ -> save(options[position].targetId) }
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(button(strings.widgetConfigCancel) { finish() })
        setContentView(root)
    }

    private fun save(targetId: Long?) {
        WidgetPrefs.setTarget(this, widgetId, targetId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        // A widget with a configuration activity gets no automatic first update: render it ourselves.
        WidgetUpdater.requestRefresh(applicationContext, intArrayOf(widgetId))
        finish()
    }

    // ---- tiny view helpers ----

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun root(loading: Boolean, title: String, body: String?): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1D1A39.toInt())
            setPadding(dp(20), dp(20), dp(20), dp(20))
            if (loading) gravity = Gravity.CENTER
        }
        if (!loading) {
            layout.addView(TextView(this).apply {
                text = title
                setTextColor(0xFFF6EFEE.toInt())
                textSize = 22f
                setPadding(0, 0, 0, dp(12))
            })
            if (body != null) {
                layout.addView(TextView(this).apply {
                    text = body
                    setTextColor(0xFFD6CCD6.toInt())
                    textSize = 15f
                    setPadding(0, 0, 0, dp(16))
                })
            }
        }
        // Android 15+ draws edge-to-edge: keep content clear of the system bars.
        layout.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                v.setPadding(dp(20) + bars.left, dp(20) + bars.top, dp(20) + bars.right, dp(20) + bars.bottom)
            }
            insets
        }
        return layout
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setTextColor(Color.WHITE)
        setOnClickListener { onClick() }
        visibility = View.VISIBLE
    }
}
