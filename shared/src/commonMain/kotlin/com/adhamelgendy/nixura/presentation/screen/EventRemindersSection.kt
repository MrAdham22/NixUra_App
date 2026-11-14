@file:OptIn(ExperimentalLayoutApi::class)

package com.adhamelgendy.nixura.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.adhamelgendy.nixura.domain.notification.ReminderError
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixChip
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconButton
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.components.SectionLabel
import com.adhamelgendy.nixura.presentation.components.Stepper
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.util.formatLead

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

/** Quick-add presets offered under the list (the ones the event does not have yet). */
private val QUICK_PRESETS = listOf(0L, 10 * MINUTE, HOUR, DAY, 3 * DAY, 7 * DAY)

/** What the OS currently allows, re-read every time the screen resumes (the user may come back from system settings). */
data class ReminderPermissionState(
    val notificationsAllowed: Boolean,
    val exactAlarmsAllowed: Boolean,
)

/**
 * The reminders of an event: one row per reminder with edit and remove buttons, quick-add chips,
 * an "Add reminder" button and, when the OS blocks something, an explanation with a shortcut to the right
 * system screen. Pure UI: every action is a callback, validation comes from [validate].
 *
 * @param leads the event's current reminders (lead times in millis), shown sorted.
 * @param describeTrigger formats the moment a reminder fires, or returns `null` when that moment has passed.
 * @param validate returns the broken rule for a candidate lead time, or `null`. [otherLeads] excludes the edited one.
 */
@Composable
fun EventRemindersSection(
    leads: Set<Long>,
    permission: ReminderPermissionState,
    describeTrigger: (leadMillis: Long) -> String?,
    validate: (leadMillis: Long, otherLeads: Set<Long>) -> ReminderError?,
    onAdd: (leadMillis: Long) -> Unit,
    onEdit: (oldLeadMillis: Long, newLeadMillis: Long) -> Unit,
    onRemove: (leadMillis: Long) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val c = Nix.colors
    var editing by rememberSaveable { mutableStateOf<Long?>(null) } // lead being edited
    var adding by rememberSaveable { mutableStateOf(false) }
    val sorted = leads.sorted()

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(s.remindersTitle)

        if (leads.isNotEmpty() && !permission.notificationsAllowed) {
            NoticeCard(s.reminderNotificationsOff, s.reminderOpenSettings, onOpenNotificationSettings)
        } else if (leads.isNotEmpty() && !permission.exactAlarmsAllowed) {
            NoticeCard(s.reminderExactOff, s.reminderAllowExact, onOpenExactAlarmSettings)
        }

        if (sorted.isEmpty()) {
            Text(s.remindersEmpty, style = NixType.Small, color = c.textMuted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sorted.forEach { lead ->
                    ReminderRow(
                        label = formatLead(lead, s),
                        detail = describeTrigger(lead)?.let(s.reminderFiresAt) ?: s.reminderPassed,
                        passed = describeTrigger(lead) == null,
                        onEdit = { editing = lead },
                        onRemove = { onRemove(lead) },
                    )
                }
            }
        }

        val missing = QUICK_PRESETS.filter { it !in leads && validate(it, leads) == null }
        if (missing.isNotEmpty()) {
            Text(s.quickAdd, style = NixType.SmallStrong, color = c.textMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                missing.forEach { NixChip(formatLead(it, s), selected = false, onClick = { onAdd(it) }) }
            }
        }
        NixButton(
            // Next to the quick-add chips this is the "any other time" choice, not a second "add".
            if (missing.isNotEmpty()) s.addOtherReminder else s.addReminder,
            { adding = true },
            style = NixButtonStyle.Secondary,
            icon = NixIcon.Plus,
            modifier = Modifier.fillMaxWidth(),
        )
        // The empty state already explains reminders; the hint is for when some are set.
        if (sorted.isNotEmpty()) Text(s.reminderHint, style = NixType.Small, color = c.textMuted)
    }

    if (adding) {
        ReminderEditorDialog(
            initialLead = null,
            validate = { validate(it, leads) },
            onConfirm = { adding = false; onAdd(it) },
            onDismiss = { adding = false },
        )
    }
    editing?.let { old ->
        ReminderEditorDialog(
            initialLead = old,
            validate = { validate(it, leads - old) },
            onConfirm = { new -> editing = null; if (new != old) onEdit(old, new) },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun NoticeCard(message: String, actionText: String, onAction: () -> Unit) {
    val c = Nix.colors
    NixCard(color = c.surfaceRaised, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NixIconView(NixIcon.Info, c.accentWarm, size = 20.dp)
            Spacer(Modifier.width(10.dp))
            Text(message, style = NixType.Small, color = c.textSecondary, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        NixButton(actionText, onAction, style = NixButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ReminderRow(label: String, detail: String, passed: Boolean, onEdit: () -> Unit, onRemove: () -> Unit) {
    val s = LocalStrings.current
    val c = Nix.colors
    NixCard(padding = PaddingValues(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp), shapeRadius = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = NixType.BodyStrong, color = if (passed) c.textMuted else c.textPrimary)
                Text(detail, style = NixType.Small, color = if (passed) c.accentWarm else c.textMuted)
            }
            // 48dp touch targets (the icon button draws a smaller visible circle inside).
            Box(Modifier.size(48.dp).semantics { contentDescription = "${s.editReminder}: $label" }, contentAlignment = Alignment.Center) {
                NixIconButton(NixIcon.Pencil, onEdit, size = 40.dp)
            }
            Box(Modifier.size(48.dp).semantics { contentDescription = "${s.removeReminder}: $label" }, contentAlignment = Alignment.Center) {
                NixIconButton(NixIcon.Trash, onRemove, size = 40.dp, tint = c.danger)
            }
        }
    }
}

/** Dialog to add ([initialLead] = null) or edit a reminder; Save stays disabled while [validate] reports a problem. */
@Composable
private fun ReminderEditorDialog(
    initialLead: Long?,
    validate: (Long) -> ReminderError?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    val c = Nix.colors
    // 0 minutes, 1 hours, 2 days, 3 at event time
    val start = remember { decompose(initialLead ?: HOUR) }
    var value by rememberSaveable { mutableStateOf(start.first) }
    var unit by rememberSaveable { mutableStateOf(start.second) }
    val lead = compose(value, unit)
    val error = validate(lead)

    Dialog(onDismissRequest = onDismiss) {
        NixCard(color = c.surface, padding = PaddingValues(22.dp)) {
            Text(if (initialLead == null) s.addReminder else s.editReminder, style = NixType.Heading, color = c.textPrimary)
            Spacer(Modifier.height(14.dp))
            Text(s.reminderUnitLabel, style = NixType.SmallStrong, color = c.textMuted)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(s.unitMinutes, s.unitHours, s.unitDays, s.atEventTime).forEachIndexed { index, label ->
                    NixChip(label, selected = unit == index, onClick = { unit = index })
                }
            }
            if (unit != AT_EVENT_TIME) {
                Spacer(Modifier.height(12.dp))
                Stepper(value, { value = it }, min = 1, max = 999)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = error?.let(s.reminderError) ?: formatLead(lead, s),
                style = NixType.Small,
                color = if (error != null) c.danger else c.textSecondary,
            )
            Spacer(Modifier.height(18.dp))
            // Equal heights even when one label wraps at a large font size.
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NixButton(s.cancel, onDismiss, Modifier.weight(1f).fillMaxHeight(), style = NixButtonStyle.Secondary)
                NixButton(s.reminderDialogSave, { onConfirm(lead) }, Modifier.weight(1f).fillMaxHeight(), enabled = error == null)
            }
        }
    }
}

private const val AT_EVENT_TIME = 3

/** Splits a lead time into a stepper value and a unit index (largest unit that divides it evenly). */
private fun decompose(lead: Long): Pair<Int, Int> = when {
    lead <= 0L -> 1 to AT_EVENT_TIME
    lead % DAY == 0L && lead / DAY <= 999 -> (lead / DAY).toInt() to 2
    lead % HOUR == 0L && lead / HOUR <= 999 -> (lead / HOUR).toInt() to 1
    else -> (lead / MINUTE).coerceIn(1, 999).toInt() to 0
}

private fun compose(value: Int, unit: Int): Long = when (unit) {
    0 -> value * MINUTE
    1 -> value * HOUR
    2 -> value * DAY
    else -> 0L
}
