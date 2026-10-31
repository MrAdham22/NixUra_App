@file:OptIn(ExperimentalLayoutApi::class)

package com.example.reclaimapp.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.model.EventCategory
import com.example.reclaimapp.domain.notification.NotificationPermissionGateway
import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.presentation.components.ConfirmDialog
import com.example.reclaimapp.presentation.components.DateTimePickerDialog
import com.example.reclaimapp.presentation.components.ErrorBanner
import com.example.reclaimapp.domain.widget.WidgetKind
import com.example.reclaimapp.presentation.components.AddToHomeScreenButton
import com.example.reclaimapp.presentation.components.NixButton
import com.example.reclaimapp.presentation.components.NixButtonStyle
import com.example.reclaimapp.presentation.components.NixCard
import com.example.reclaimapp.presentation.components.NixChip
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconView
import com.example.reclaimapp.presentation.components.NixTextField
import com.example.reclaimapp.presentation.components.SectionLabel
import com.example.reclaimapp.presentation.components.Stepper
import com.example.reclaimapp.presentation.components.SubScreen
import com.example.reclaimapp.presentation.events.EventsViewModel
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import com.example.reclaimapp.presentation.util.formatDateTime
import com.example.reclaimapp.presentation.util.formatLead
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR
private val LEAD_PRESETS = listOf(0L, 10 * MINUTE, HOUR, DAY, 3 * DAY, 7 * DAY)

private fun encodeLeads(leads: Set<Long>): String = leads.sorted().joinToString(",")
private fun decodeLeads(raw: String): Set<Long> = raw.split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()

@Composable
fun EventFormScreen(editId: Long?, navigator: Navigator) {
    val vm: EventsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val c = Nix.colors
    val days = koinInject<DayCalculator>()
    val clock = koinInject<Clock>()
    val permission = koinInject<NotificationPermissionGateway>()
    val editing = editId != null

    val defaultTarget = remember {
        days.startOfDayMillis(days.dayIndexOf(clock.nowEpochMillis()) + 1) + 9 * HOUR
    }
    var title by rememberSaveable { mutableStateOf("") }
    var categoryOrdinal by rememberSaveable { mutableStateOf(EventCategory.PERSONAL.ordinal) }
    var target by rememberSaveable { mutableStateOf(defaultTarget) }
    var leadsCsv by rememberSaveable { mutableStateOf("") }
    var loadedEdit by rememberSaveable { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var customValue by rememberSaveable { mutableStateOf(1) }
    var customUnit by rememberSaveable { mutableStateOf(1) } // 0 min, 1 hour, 2 day

    val leads = decodeLeads(leadsCsv)
    val category = EventCategory.entries[categoryOrdinal]

    LaunchedEffect(editId, state.events) {
        if (editId != null && !loadedEdit) {
            state.events.firstOrNull { it.id == editId }?.let {
                title = it.title
                categoryOrdinal = it.category.ordinal
                target = it.targetAtEpochMillis
                leadsCsv = encodeLeads(it.reminderLeadMillis.toSet())
                loadedEdit = true
            }
        }
    }

    SubScreen(
        title = if (editing) s.editEvent else s.newEvent,
        onBack = { navigator.back() },
    ) {
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }

        NixTextField(title, { title = it }, label = s.eventNameLabel, placeholder = s.eventNamePlaceholder, maxLength = 60)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(s.eventCategoryLabel)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EventCategory.entries.forEach {
                    NixChip(s.categoryLabel(it), selected = it == category, onClick = { categoryOrdinal = it.ordinal })
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(s.eventWhenLabel)
            NixCard(onClick = { showPicker = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NixIconView(NixIcon.Calendar, c.accent, size = 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(formatDateTime(target, days, s), style = NixType.BodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f))
                    NixIconView(NixIcon.Chevron, c.textMuted, size = 18.dp)
                }
            }
            if (target <= clock.nowEpochMillis()) {
                Text(s.eventPastWarning, style = NixType.Small, color = c.accentWarm)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(s.remindMe)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (LEAD_PRESETS + leads).distinct().sorted().forEach { lead ->
                    NixChip(
                        formatLead(lead, s),
                        selected = lead in leads,
                        onClick = { leadsCsv = encodeLeads(if (lead in leads) leads - lead else leads + lead) },
                    )
                }
            }
            NixCard {
                Text(s.customReminder, style = NixType.BodyStrong, color = c.textPrimary)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stepper(customValue, { customValue = it }, min = 1, max = 999)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(s.unitMinutes, s.unitHours, s.unitDays).forEachIndexed { index, label ->
                        NixChip(label, selected = customUnit == index, onClick = { customUnit = index })
                    }
                    Spacer(Modifier.weight(1f))
                    NixButton(
                        s.add,
                        {
                            val unit = when (customUnit) { 0 -> MINUTE; 1 -> HOUR; else -> DAY }
                            leadsCsv = encodeLeads(leads + customValue * unit)
                        },
                        style = NixButtonStyle.Secondary,
                    )
                }
            }
            Text(s.reminderHint, style = NixType.Small, color = c.textMuted)
        }

        NixButton(
            text = if (editing) s.saveChanges else s.addEvent,
            onClick = {
                if (leads.isNotEmpty() && !permission.isGranted()) permission.request()
                vm.onSave(editId, title, category, target, leads.toList()) { navigator.back() }
            },
            enabled = title.isNotBlank(),
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        if (editing && editId != null) {
            AddToHomeScreenButton(WidgetKind.EVENT, editId)
        }
        if (editing) {
            NixButton(s.deleteEvent, { confirmDelete = true }, style = NixButtonStyle.Danger, icon = NixIcon.Trash, modifier = Modifier.fillMaxWidth())
        }
    }

    if (showPicker) {
        DateTimePickerDialog(
            initialEpochMillis = target,
            onConfirm = { target = it; showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
    if (confirmDelete && editId != null) {
        ConfirmDialog(
            title = s.deleteEventTitle,
            message = s.deleteEventBody,
            confirmText = s.delete,
            cancelText = s.cancel,
            danger = true,
            onConfirm = { confirmDelete = false; vm.onDelete(editId) { navigator.back() } },
            onDismiss = { confirmDelete = false },
        )
    }
}
