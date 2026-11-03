package com.example.reclaimapp.presentation.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.domain.coach.BadHabitSnapshot
import com.example.reclaimapp.domain.coach.durationText
import com.example.reclaimapp.domain.model.CoachConversation
import com.example.reclaimapp.domain.model.CoachRole
import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.presentation.coach.CoachViewModel
import com.example.reclaimapp.presentation.components.ConfirmDialog
import com.example.reclaimapp.presentation.components.ErrorBanner
import com.example.reclaimapp.presentation.components.NixCard
import com.example.reclaimapp.presentation.components.NixChip
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconButton
import com.example.reclaimapp.presentation.components.NixIconView
import com.example.reclaimapp.presentation.components.NixTextField
import com.example.reclaimapp.presentation.components.SubScreen
import com.example.reclaimapp.presentation.components.pressable
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import com.example.reclaimapp.presentation.theme.NixUraPalette
import com.example.reclaimapp.presentation.util.formatDate
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val QuickActionIcons = listOf(NixIcon.Flame, NixIcon.Sparkle, NixIcon.Reset, NixIcon.Shield)

@Composable
fun CoachScreen(navigator: Navigator) {
    val vm: CoachViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val c = Nix.colors
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    // Until the user writes something, a short welcome replaces the chat.
    val fresh = state.messages.none { it.role == CoachRole.USER } && !state.isTyping

    LaunchedEffect(Unit) { vm.onScreenShown() }

    LaunchedEffect(state.messages.size, state.isTyping) {
        val last = state.messages.size + (if (state.isTyping) 1 else 0) - 1
        if (last >= 0 && !fresh) listState.animateScrollToItem(last)
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
            onHistory = { navigator.push(Route.CoachHistory) },
            onNewChat = { input = ""; vm.onNewChat() },
        )
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError, Modifier.padding(horizontal = 20.dp)) }

        if (fresh) {
            CoachWelcome(
                greeting = state.messages.firstOrNull()?.text.orEmpty(),
                habits = state.habits,
                suggestions = state.suggestions,
                onSuggestion = ::send,
                onWhatHappens = { input = s.coachWhatHappensPrefix },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            state.insight?.let { insight ->
                Column(Modifier.padding(horizontal = 20.dp)) {
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
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    val mine = message.role == CoachRole.USER
                    Row(Modifier.fillMaxWidth().animateItem(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Box(
                            Modifier
                                .widthIn(max = 320.dp)
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (mine) 20.dp else 6.dp, bottomEnd = if (mine) 6.dp else 20.dp))
                                .background(if (mine) c.accent else c.surface)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                coachRichText(message.text, if (mine) c.onAccent else c.accentWarm),
                                style = NixType.Body,
                                color = if (mine) c.onAccent else c.textPrimary,
                            )
                        }
                    }
                }
                if (state.isTyping) {
                    item(key = "typing") { TypingDots() }
                }
            }
            if (!state.isTyping) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Starts the "what happens if I stop for ..." question; the user finishes it with a time.
                    NixChip(s.coachQuickWhatHappens, selected = false, onClick = { input = s.coachWhatHappensPrefix })
                    state.suggestions.forEach { suggestion ->
                        NixChip(suggestion, selected = false, onClick = { send(suggestion) })
                    }
                }
            }
        }

        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
                haptic = true,
            )
        }
    }
}

/** Past chats, newest first. Opening one continues it. */
@Composable
fun CoachHistoryScreen(navigator: Navigator) {
    val vm: CoachViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val days = koinInject<DayCalculator>()
    val s = LocalStrings.current
    val c = Nix.colors
    var confirmDelete by remember { mutableStateOf<Long?>(null) }

    SubScreen(
        title = s.coachHistoryTitle,
        onBack = { navigator.back() },
        actions = {
            NixIconButton(NixIcon.Plus, { vm.onNewChat(); navigator.back() }, tint = c.onAccent, background = c.accent, bordered = false, size = 40.dp)
        },
    ) {
        if (state.conversations.isEmpty()) {
            Text(s.coachHistoryEmpty, style = NixType.Body, color = c.textSecondary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.conversations.forEach { chat ->
                ChatRow(
                    chat,
                    date = formatDate(days.dayIndexOf(chat.updatedAtEpochMillis), s),
                    active = chat.id == state.activeChatId,
                    onOpen = { vm.onOpenChat(chat.id); navigator.back() },
                    onDelete = { confirmDelete = chat.id },
                )
            }
        }
    }

    confirmDelete?.let { id ->
        ConfirmDialog(
            title = s.coachDeleteChatTitle,
            message = s.coachDeleteChatBody,
            confirmText = s.remove,
            cancelText = s.cancel,
            danger = true,
            onConfirm = { confirmDelete = null; vm.onDeleteChat(id) },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun ChatRow(chat: CoachConversation, date: String, active: Boolean, onOpen: () -> Unit, onDelete: () -> Unit) {
    val s = LocalStrings.current
    val c = Nix.colors
    NixCard(
        Modifier.fillMaxWidth(),
        onClick = onOpen,
        borderColor = if (active) c.accent else c.border,
        padding = PaddingValues(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        shapeRadius = 18.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(chat.title.ifBlank { s.coachUntitledChat }, style = NixType.BodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(chat.preview.replace("**", ""), style = NixType.Small, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(date, style = NixType.Smaller, color = c.textMuted)
            }
            Spacer(Modifier.width(6.dp))
            NixIconButton(NixIcon.Trash, onDelete, tint = c.textMuted, background = Color.Transparent, bordered = false, size = 40.dp)
        }
    }
}

/** The first thing you see: one line that matters, your numbers, and big obvious ways to start. */
@Composable
private fun CoachWelcome(
    greeting: String,
    habits: List<BadHabitSnapshot>,
    suggestions: List<String>,
    onSuggestion: (String) -> Unit,
    onWhatHappens: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val c = Nix.colors
    val best = habits.maxByOrNull { it.elapsed }
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(NixUraPalette.Purple, NixUraPalette.Plum, NixUraPalette.Rose)))
                .padding(22.dp),
        ) {
            NixIconView(NixIcon.Sparkle, NixUraPalette.Orange, size = 26.dp)
            Spacer(Modifier.height(12.dp))
            Text(coachRichText(greeting, NixUraPalette.Orange), style = NixType.Title, color = NixUraPalette.Blush)
            Spacer(Modifier.height(6.dp))
            Text(s.coachWelcomeSubtitle, style = NixType.Small, color = NixUraPalette.Blush.copy(alpha = 0.75f))
            if (best != null) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WelcomeStat(s.coachStatBest, durationText(best.elapsed), Modifier.weight(1.4f))
                    WelcomeStat(s.coachStatHabits, habits.size.toString(), Modifier.weight(1f))
                }
            }
        }
        Text(s.coachPickStart, style = NixType.Heading, color = c.textPrimary)
        suggestions.take(4).chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { column, suggestion ->
                    QuickAction(QuickActionIcons[row * 2 + column], suggestion, Modifier.weight(1f)) { onSuggestion(suggestion) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        NixChip(s.coachQuickWhatHappens, selected = false, onClick = onWhatHappens)
    }
}

@Composable
private fun WelcomeStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(value, style = NixType.Heading, color = NixUraPalette.Blush, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = NixType.Small, color = NixUraPalette.Blush.copy(alpha = 0.75f), maxLines = 1)
    }
}

@Composable
private fun QuickAction(icon: NixIcon, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Nix.colors
    Column(
        modifier
            .pressable(pressedScale = 0.96f, haptic = true, onClick = onClick)
            .heightIn(min = 92.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(c.surface)
            .padding(14.dp),
    ) {
        NixIconView(icon, c.accentWarm, size = 22.dp)
        Spacer(Modifier.height(10.dp))
        Text(label, style = NixType.BodyStrong, color = c.textPrimary)
    }
}

/** Coach text with `**key words**` picked out, so the eye lands on what matters. */
private fun coachRichText(text: String, highlight: Color): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { index, part ->
        if (index % 2 == 1) withStyle(SpanStyle(color = highlight, fontWeight = FontWeight.Bold)) { append(part) } else append(part)
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

/** Compact header: back button, title with the mode line underneath, chat history and new chat. */
@Composable
private fun CoachHeader(title: String, onBack: () -> Unit, onHistory: () -> Unit, onNewChat: () -> Unit) {
    val c = Nix.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NixIconButton(NixIcon.Back, onBack, size = 40.dp, background = c.surface.copy(alpha = 0.6f), bordered = false)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = NixType.Heading, color = c.textPrimary, maxLines = 1)
            CoachModeLine()
        }
        NixIconButton(NixIcon.Clock, onHistory, tint = c.textSecondary, size = 40.dp, background = c.surface.copy(alpha = 0.6f), bordered = false)
        Spacer(Modifier.width(8.dp))
        NixIconButton(NixIcon.Plus, onNewChat, tint = c.textSecondary, size = 40.dp, background = c.surface.copy(alpha = 0.6f), bordered = false)
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
        padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
                                .pressable(pressedScale = 0.98f) { onPick(habit.id); open = false }
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) c.accent.copy(alpha = 0.25f) else c.surface)
                                .border(1.dp, if (selected) c.accent else c.border, RoundedCornerShape(14.dp))
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
