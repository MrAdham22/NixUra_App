package com.example.reclaimapp.presentation.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.domain.coach.BadHabitSnapshot
import com.example.reclaimapp.domain.coach.durationText
import com.example.reclaimapp.domain.model.CoachRole
import com.example.reclaimapp.presentation.coach.CoachViewModel
import com.example.reclaimapp.presentation.components.ConfirmDialog
import com.example.reclaimapp.presentation.components.ErrorBanner
import com.example.reclaimapp.presentation.components.NixCard
import com.example.reclaimapp.presentation.components.NixChip
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconButton
import com.example.reclaimapp.presentation.components.NixIconView
import com.example.reclaimapp.presentation.components.NixTextField
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CoachScreen(navigator: Navigator) {
    val vm: CoachViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val c = Nix.colors
    var input by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { vm.onScreenShown() }

    LaunchedEffect(state.messages.size, state.isTyping) {
        val last = state.messages.size + (if (state.isTyping) 1 else 0) - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    fun send(text: String) {
        if (text.isBlank() || state.isTyping) return
        vm.onSend(text)
        input = ""
    }

    Column(Modifier.fillMaxSize()) {
        CoachHeader(
            title = s.coachTitle,
            onBack = { navigator.back() },
            onClear = { confirmClear = true },
        )
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.insight?.let { insight ->
                FocusPicker(
                    label = s.todaysFocus,
                    pickHint = s.coachPickHabit,
                    title = insight.title,
                    body = insight.body,
                    habits = state.habits,
                    focusHabitId = state.focusHabitId,
                    onPick = vm::onSelectHabit,
                )
            }
            state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.messages, key = { it.id }) { message ->
                val mine = message.role == CoachRole.USER
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Box(
                        Modifier
                            .widthIn(max = 320.dp)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (mine) 20.dp else 6.dp, bottomEnd = if (mine) 6.dp else 20.dp))
                            .background(if (mine) c.accent else c.surface)
                            .border(1.dp, if (mine) androidx.compose.ui.graphics.Color.Transparent else c.border, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(message.text, style = NixType.Body, color = if (mine) c.onAccent else c.textPrimary)
                    }
                }
            }
            if (state.isTyping) {
                item(key = "typing") { TypingDots() }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.suggestions.isNotEmpty() && !state.isTyping) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.suggestions.forEach { suggestion ->
                        NixChip(suggestion, selected = false, onClick = { send(suggestion) })
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NixTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = s.coachInputHint,
                    maxLength = 500,
                    modifier = Modifier.weight(1f),
                )
                NixIconButton(
                    NixIcon.Send,
                    { send(input) },
                    size = 50.dp,
                    tint = c.onAccent,
                    background = if (input.isBlank() || state.isTyping) c.accent.copy(alpha = 0.4f) else c.accent,
                    bordered = false,
                )
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = s.clearChatTitle,
            message = s.clearChatBody,
            confirmText = s.clear,
            cancelText = s.cancel,
            danger = true,
            onConfirm = { confirmClear = false; vm.onClear() },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun TypingDots() {
    val c = Nix.colors
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600, delayMillis = index * 150, easing = LinearEasing), RepeatMode.Reverse),
                label = "dot$index",
            )
            Box(Modifier.size(8.dp).alpha(alpha).clip(CircleShape).background(c.textSecondary))
        }
    }
}

/** Compact header: back button, title with the mode line underneath, clear-chat button. */
@Composable
private fun CoachHeader(title: String, onBack: () -> Unit, onClear: () -> Unit) {
    val c = Nix.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NixIconButton(NixIcon.Back, onBack, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = NixType.Heading, color = c.textPrimary, maxLines = 1)
            CoachModeLine()
        }
        NixIconButton(NixIcon.Trash, onClear, tint = c.textSecondary, size = 40.dp)
    }
}

/** Isolated subtitle slot. Mission 3 replaces its text with the Offline / AI mode label. */
@Composable
private fun CoachModeLine() {
    Text(LocalStrings.current.coachModeOfflineShort, style = NixType.Small, color = Nix.colors.textMuted, maxLines = 1)
}

/** One-line "today's focus" strip. Tap it to open the list of quit habits and pick the one to talk about. */
@Composable
private fun FocusPicker(
    label: String,
    pickHint: String,
    title: String,
    body: String,
    habits: List<BadHabitSnapshot>,
    focusHabitId: Long?,
    onPick: (Long) -> Unit,
) {
    val c = Nix.colors
    var open by rememberSaveable { mutableStateOf(false) }
    NixCard(
        color = c.accent.copy(alpha = 0.10f),
        borderColor = c.accent.copy(alpha = 0.35f),
        padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        onClick = { open = !open },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NixIconView(NixIcon.Sparkle, c.accent, size = 16.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                label.uppercase() + " · " + title,
                style = NixType.Eyebrow,
                color = c.accent,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            NixIconView(NixIcon.Chevron, c.accent, size = 16.dp, modifier = Modifier.rotate(if (open) -90f else 90f))
        }
        if (open) {
            Spacer(Modifier.height(8.dp))
            Text(body, style = NixType.Small, color = c.textPrimary)
            if (habits.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(pickHint, style = NixType.Eyebrow, color = c.textMuted)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    habits.forEach { habit ->
                        val selected = habit.id == focusHabitId
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) c.accent.copy(alpha = 0.25f) else c.surface)
                                .border(1.dp, if (selected) c.accent else c.border, RoundedCornerShape(14.dp))
                                .clickable { onPick(habit.id); open = false }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(habit.name, style = NixType.Body, color = c.textPrimary, maxLines = 1, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text(durationText(habit.elapsed), style = NixType.Small, color = c.textMuted, maxLines = 1)
                            if (selected) {
                                Spacer(Modifier.width(8.dp))
                                NixIconView(NixIcon.CircleCheck, c.accent, size = 16.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
