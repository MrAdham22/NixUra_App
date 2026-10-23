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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.reclaimapp.presentation.components.ScreenHeader
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
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader(
                eyebrow = s.coachEyebrow,
                title = s.coachTitle,
                trailing = { NixIconButton(NixIcon.Trash, { confirmClear = true }, tint = c.textSecondary) },
            )
            state.insight?.let { insight ->
                NixCard(color = c.accent.copy(alpha = 0.10f), borderColor = c.accent.copy(alpha = 0.35f), padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        NixIconView(NixIcon.Sparkle, c.accent, size = 18.dp)
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(s.todaysFocus.uppercase() + " · " + insight.title, style = NixType.Eyebrow, color = c.accent, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Text(insight.body, style = NixType.Small, color = c.textPrimary)
                        }
                    }
                }
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

        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.suggestions.isNotEmpty()) {
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
            Text(s.coachOfflineNote, style = NixType.Small, color = c.textMuted)
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
