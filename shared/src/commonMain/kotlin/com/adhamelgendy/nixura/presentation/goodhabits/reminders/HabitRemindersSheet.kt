package com.adhamelgendy.nixura.presentation.goodhabits.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.notification.ReminderCapabilities
import com.adhamelgendy.nixura.domain.reminder.HabitReminderRules
import com.adhamelgendy.nixura.domain.reminder.LocationAccess
import com.adhamelgendy.nixura.domain.reminder.ReminderDeviceState
import com.adhamelgendy.nixura.domain.reminder.ReminderStrategy
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixChip
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.components.NixSheet
import com.adhamelgendy.nixura.presentation.components.NixSwitch
import com.adhamelgendy.nixura.presentation.components.SectionLabel
import com.adhamelgendy.nixura.presentation.components.Text
import com.adhamelgendy.nixura.presentation.components.TimePickerDialog
import com.adhamelgendy.nixura.presentation.components.rememberNotificationAsk
import com.adhamelgendy.nixura.presentation.deletion.SheetAction
import com.adhamelgendy.nixura.presentation.screen.NoticeCard
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.strings.Strings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.util.formatClockMinutes
import com.adhamelgendy.nixura.presentation.util.formatDate
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Opens a good habit's reminder controls. Get one with [rememberHabitReminders]. */
class HabitReminders internal constructor(
    private val status: (Long) -> String?,
    private val open: (Long) -> Unit,
) {
    /** The row for the long-press sheet ("Reminders: On · every 2 h"), or null while the habit is not loaded yet. */
    fun sheetAction(habitId: Long): SheetAction? = status(habitId)?.let { SheetAction(it, NixIcon.Bell) { open(habitId) } }
}

/** The reminder controls of good habits: a row in the long-press sheet that opens [HabitRemindersSheet]. */
@Composable
fun rememberHabitReminders(): HabitReminders {
    val s = LocalStrings.current
    val vm: HabitRemindersViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val clock = koinInject<Clock>()
    val days = koinInject<DayCalculator>()
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }

    openId?.let { id ->
        val habit = state.habits[id]
        if (habit == null) openId = null else HabitRemindersSheet(habit, state, vm, onDismiss = { openId = null })
    }
    val current by androidx.compose.runtime.rememberUpdatedState(state)
    return remember(s) {
        HabitReminders(
            status = { id -> current.habits[id]?.let { s.reminderSheetRow(reminderStatusText(it, current.device, clock.nowEpochMillis(), days, s)) } },
            open = { openId = it },
        )
    }
}

/** "Off", "Paused until Sat", "Needs your location", "On · every 2 h", "On · 7:00 PM"... */
fun reminderStatusText(habit: GoodHabit, device: ReminderDeviceState, now: Long, days: DayCalculator, s: Strings): String {
    if (!HabitReminderRules.isEnabled(habit)) return s.reminderStatusOff
    val paused = maxOf(habit.reminder.pausedUntilEpochMillis ?: 0L, device.pausedUntilEpochMillis ?: 0L)
    if (paused > now) return s.reminderStatusPausedUntil(formatDate(days.dayIndexOf(paused), s, includeWeekday = true, includeYear = false))
    return when (HabitReminderRules.strategyOf(habit)) {
        ReminderStrategy.PRAYER -> if (device.location == null) s.reminderStatusNeedsLocation else s.reminderStatusPrayer
        ReminderStrategy.WATER -> s.reminderStatusWater
        ReminderStrategy.DAILY_TIME, ReminderStrategy.WEEKLY_TIME -> s.reminderStatusAt(formatClockMinutes(HabitReminderRules.timeOf(habit), s))
    }
}

private enum class TimeField { TIME, FROM, UNTIL }

@Composable
private fun HabitRemindersSheet(habit: GoodHabit, state: HabitRemindersUiState, vm: HabitRemindersViewModel, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val c = Nix.colors
    val clock = koinInject<Clock>()
    val days = koinInject<DayCalculator>()
    val capabilities = koinInject<ReminderCapabilities>()
    val ask = rememberNotificationAsk()
    val strategy = HabitReminderRules.strategyOf(habit)
    val enabled = HabitReminderRules.isEnabled(habit)
    var editing by remember { mutableStateOf<TimeField?>(null) }
    // Re-read what the OS allows when the user comes back from a system dialog or settings.
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }
    val notificationsAllowed = remember(resumeTick, enabled) { capabilities.notificationsAllowed() }
    val channelOn = remember(resumeTick, enabled) { capabilities.channelEnabled(NotificationKind.GOOD_HABIT) }

    NixSheet(onDismiss) {
        Text(habit.name, style = NixType.Heading, color = c.textPrimary)
        Spacer(Modifier.height(4.dp))
        SectionLabel(s.remindersTitle)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NixCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NixIconView(NixIcon.Bell, c.accent, size = 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(s.reminderSwitch, style = NixType.BodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f))
                    NixSwitch(enabled, { on ->
                        if (on) ask.ensure(s.notificationAskHabitReason)
                        vm.setEnabled(habit, on)
                    })
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    when (strategy) {
                        ReminderStrategy.PRAYER -> s.reminderExplainPrayer
                        ReminderStrategy.WATER -> s.reminderExplainWater
                        ReminderStrategy.DAILY_TIME -> s.reminderExplainDaily
                        ReminderStrategy.WEEKLY_TIME -> s.reminderExplainWeekly
                    },
                    style = NixType.Small,
                    color = c.textMuted,
                )
            }

            if (enabled) {
                // What the OS blocks, with the screen that fixes it (phase B's capabilities).
                when {
                    !notificationsAllowed -> NoticeCard(s.habitReminderNotificationsOff, s.reminderOpenSettings, capabilities::openNotificationSettings)
                    !channelOn ->
                        NoticeCard(s.reminderChannelOff, s.turnOnInSettings) { capabilities.openChannelSettings(NotificationKind.GOOD_HABIT) }
                }

                when (strategy) {
                    ReminderStrategy.PRAYER -> LocationCard(state, vm, resumeTick)
                    ReminderStrategy.WATER -> {
                        val window = HabitReminderRules.waterWindowOf(habit)
                        TimeRow(s.reminderFrom, formatClockMinutes(window.first, s)) { editing = TimeField.FROM }
                        TimeRow(s.reminderUntil, formatClockMinutes(window.last, s)) { editing = TimeField.UNTIL }
                    }
                    ReminderStrategy.DAILY_TIME, ReminderStrategy.WEEKLY_TIME -> {
                        val time = HabitReminderRules.timeOf(habit)
                        TimeRow(s.reminderTime, formatClockMinutes(time, s)) { editing = TimeField.TIME }
                        if (HabitReminderRules.isQuiet(time)) Text(s.reminderTimeIsQuiet, style = NixType.Small, color = c.accentWarm)
                    }
                }
                Text(
                    s.reminderQuietNote(formatClockMinutes(HabitReminderRules.QUIET_START_MINUTES, s), formatClockMinutes(HabitReminderRules.QUIET_END_MINUTES, s)),
                    style = NixType.Small,
                    color = c.textMuted,
                )

                val pausedUntil = habit.reminder.pausedUntilEpochMillis?.takeIf { it > clock.nowEpochMillis() }
                if (pausedUntil != null) {
                    Text(
                        s.reminderStatusPausedUntil(formatDate(days.dayIndexOf(pausedUntil), s, includeWeekday = true, includeYear = false)),
                        style = NixType.SmallStrong,
                        color = c.accentWarm,
                    )
                    NixButton(s.reminderResume, { vm.resume(habit) }, style = NixButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NixChip(s.reminderPauseToday, selected = false, onClick = { vm.pauseToday(habit) })
                        NixChip(s.reminderPauseWeek, selected = false, onClick = { vm.pauseWeek(habit) })
                    }
                }
            }
        }
    }

    editing?.let { field ->
        val window = HabitReminderRules.waterWindowOf(habit)
        TimePickerDialog(
            initialMinutes = when (field) {
                TimeField.TIME -> HabitReminderRules.timeOf(habit)
                TimeField.FROM -> window.first
                TimeField.UNTIL -> window.last
            },
            title = when (field) {
                TimeField.TIME -> s.reminderTime
                TimeField.FROM -> s.reminderFrom
                TimeField.UNTIL -> s.reminderUntil
            },
            onConfirm = { minutes ->
                when (field) {
                    TimeField.TIME -> vm.setTime(habit, minutes)
                    TimeField.FROM -> vm.setWindow(habit, minutes, window.last)
                    TimeField.UNTIL -> vm.setWindow(habit, window.first, minutes)
                }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun TimeRow(label: String, value: String, onClick: () -> Unit) {
    val c = Nix.colors
    NixCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NixIconView(NixIcon.Clock, c.accent, size = 22.dp)
            Spacer(Modifier.width(12.dp))
            Text(label, style = NixType.BodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f))
            Text(value, style = NixType.BodyStrong, color = c.accent)
        }
    }
}

/** Prayer times need the phone's location: why, and the button that reads it (or updates it). */
@Composable
private fun LocationCard(state: HabitRemindersUiState, vm: HabitRemindersViewModel, resumeTick: Int) {
    val s = LocalStrings.current
    val c = Nix.colors
    val access = remember(resumeTick, state.location) { vm.locationAccess() }
    val saved = state.device.location != null
    NixCard(color = c.surfaceRaised) {
        Text(if (saved) s.locationSaved else s.locationTitle, style = NixType.BodyStrong, color = c.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                state.location == LocationReading.READING -> s.locationReading
                state.location == LocationReading.FAILED -> s.locationFailed
                access == LocationAccess.OPEN_SETTINGS -> s.locationBlocked
                else -> s.locationBody
            },
            style = NixType.Small,
            color = if (state.location == LocationReading.FAILED || access == LocationAccess.OPEN_SETTINGS) c.accentWarm else c.textMuted,
        )
        if (access != LocationAccess.UNAVAILABLE) {
            Spacer(Modifier.height(12.dp))
            NixButton(
                text = when {
                    access == LocationAccess.OPEN_SETTINGS -> s.reminderOpenSettings
                    saved -> s.locationUpdate
                    else -> s.locationUse
                },
                onClick = vm::useLocation,
                style = if (saved) NixButtonStyle.Secondary else NixButtonStyle.Primary,
                loading = state.location == LocationReading.READING,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
