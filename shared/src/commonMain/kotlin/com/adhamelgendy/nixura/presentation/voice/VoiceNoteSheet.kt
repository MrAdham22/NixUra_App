package com.adhamelgendy.nixura.presentation.voice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adhamelgendy.nixura.domain.coach.ai.CoachAction
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconButton
import com.adhamelgendy.nixura.presentation.components.NixIconView
import com.adhamelgendy.nixura.presentation.components.NixSheet
import com.adhamelgendy.nixura.presentation.components.pressable
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.platform.rememberReducedMotion
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.strings.Strings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.util.formatClockMinutes
import com.adhamelgendy.nixura.presentation.util.formatDate
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/** Shows the voice note sheet whenever the shared [VoiceNoteViewModel] is not idle. Placed once, in the app shell. */
@Composable
fun VoiceNoteHost(navigator: Navigator) {
    val vm: VoiceNoteViewModel = koinViewModel()
    val stage by vm.stage.collectAsStateWithLifecycle()
    if (stage == VoiceStage.Idle) return
    val s = LocalStrings.current

    if (stage == VoiceStage.NeedsPermission) {
        // The system's question is outside the app; look again until the user has answered it.
        LaunchedEffect(Unit) {
            while (true) {
                delay(400)
                vm.onPermissionMaybeGranted()
            }
        }
    }

    NixSheet(onDismiss = vm::onClose) {
        AnimatedContent(
            targetState = stage,
            // The edit form fades in over the list like a new step.
            contentKey = { if (it is VoiceStage.Review && it.editing != null) "edit" else it::class },
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "voice",
        ) { current ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (current) {
                    VoiceStage.Idle -> Unit
                    VoiceStage.NeedsSignIn -> Message(NixIcon.User, s.voiceSignInTitle, s.voiceSignInBody, s.authSignIn, {
                        vm.onClose()
                        navigator.push(Route.Auth.Login)
                    })
                    VoiceStage.NeedsConsent -> AiConsentContent(onAgree = vm::onConsent, onDecline = vm::onClose)
                    VoiceStage.NeedsPermission -> Message(NixIcon.Mic, s.voiceAllowMicTitle, s.voiceAllowMicBody, s.voiceAllowMic, vm::onAskPermission)
                    is VoiceStage.Recording -> {
                        Text(s.voiceListening, style = NixType.Heading, color = Nix.colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(s.voiceExample, style = NixType.Small, color = Nix.colors.textMuted, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(20.dp))
                        MicOrb(level = current.level, busy = false, icon = NixIcon.Stop, onClick = vm::onStop)
                        Spacer(Modifier.height(14.dp))
                        Text("${current.seconds / 60}:${(current.seconds % 60).toString().padStart(2, '0')}", style = NixType.Title, color = Nix.colors.accentWarm)
                        Text(s.voiceTapToStop, style = NixType.Small, color = Nix.colors.textSecondary)
                        Spacer(Modifier.height(8.dp))
                    }
                    VoiceStage.Understanding -> {
                        Text(s.voiceUnderstanding, style = NixType.Heading, color = Nix.colors.textPrimary)
                        Spacer(Modifier.height(20.dp))
                        MicOrb(level = 0.3f, busy = true, icon = NixIcon.Sparkle, onClick = null)
                        Spacer(Modifier.height(14.dp))
                        Text(s.voiceUnderstandingHint, style = NixType.Small, color = Nix.colors.textSecondary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                    }
                    is VoiceStage.Review -> ReviewList(current, vm, s)
                    is VoiceStage.Done -> {
                        val body = if (current.failed > 0) s.voiceDoneFailed(current.failed) else s.voiceDoneBody
                        Message(NixIcon.Tick, s.voiceDoneTitle(current.added), body, s.done, vm::onClose)
                        if (vm.supported) {
                            Spacer(Modifier.height(8.dp))
                            NixButton(s.voiceRecordAnother, vm::onRetry, Modifier.fillMaxWidth(), style = NixButtonStyle.Secondary, icon = NixIcon.Mic)
                        }
                    }
                    is VoiceStage.Failed -> {
                        val text = when (current.reason) {
                            VoiceFailure.Microphone -> s.voiceFailMic
                            VoiceFailure.NothingHeard -> s.voiceFailNothingHeard
                            VoiceFailure.NothingToAdd -> s.voiceFailNothingToAdd
                            VoiceFailure.LimitReached -> s.voiceFailLimit
                            VoiceFailure.AiResting -> s.voiceFailResting
                            VoiceFailure.Offline -> s.voiceFailOffline
                        }
                        val canRetry = current.reason != VoiceFailure.LimitReached
                        Message(NixIcon.Info, s.voiceFailTitle, text, if (canRetry) s.voiceTryAgain else s.done, if (canRetry) vm::onRetry else vm::onClose)
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(icon: NixIcon, title: String, body: String, action: String, onAction: () -> Unit) {
    val c = Nix.colors
    Box(Modifier.size(64.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        NixIconView(icon, c.accent, size = 30.dp)
    }
    Spacer(Modifier.height(14.dp))
    Text(title, style = NixType.Heading, color = c.textPrimary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(body, style = NixType.Body, color = c.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(18.dp))
    NixButton(action, onAction, Modifier.fillMaxWidth())
}

/** The "may NixUra use online AI?" question. Shared by the coach chat and the voice note sheet. */
@Composable
fun AiConsentContent(onAgree: () -> Unit, onDecline: () -> Unit) {
    val c = Nix.colors
    val s = LocalStrings.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            NixIconView(NixIcon.Sparkle, c.accent, size = 30.dp)
        }
        Spacer(Modifier.height(14.dp))
        Text(s.aiConsentTitle, style = NixType.Heading, color = c.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            s.aiConsentPoints.forEach { point ->
                Row {
                    NixIconView(NixIcon.Tick, c.accentWarm, Modifier.padding(top = 2.dp), size = 16.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(point, style = NixType.Small, color = c.textSecondary)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        NixButton(s.aiConsentAgree, onAgree, Modifier.fillMaxWidth(), icon = NixIcon.Sparkle)
        Spacer(Modifier.height(8.dp))
        NixButton(s.aiConsentDecline, onDecline, Modifier.fillMaxWidth(), style = NixButtonStyle.Secondary)
    }
}

/** The round microphone: rings breathe outwards, and the core swells with the user's voice. */
@Composable
private fun MicOrb(level: Float, busy: Boolean, icon: NixIcon, onClick: (() -> Unit)?) {
    val c = Nix.colors
    val reduced = rememberReducedMotion()
    val loop = rememberInfiniteTransition(label = "orb")
    val wave by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(if (busy) 1100 else 1800, easing = LinearEasing), RepeatMode.Restart), label = "wave")
    val swell by animateFloatAsState(level, spring(dampingRatio = 0.6f, stiffness = 300f), label = "swell")
    Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(180.dp)) {
            val core = 46.dp.toPx()
            if (!reduced) {
                // Two rings, half a cycle apart, fading as they grow.
                for (offset in listOf(0f, 0.5f)) {
                    val t = (wave + offset) % 1f
                    drawCircle(c.accent.copy(alpha = (1f - t) * 0.28f), radius = core + (size.minDimension / 2 - core) * t)
                }
            }
            drawCircle(c.accentWarm.copy(alpha = 0.22f), radius = core + 26.dp.toPx() * swell)
            if (busy && !reduced) {
                val w = 4.dp.toPx()
                val r = core + 10.dp.toPx()
                drawArc(
                    c.accentWarm, wave * 360f, 90f, false,
                    topLeft = center.copy(x = center.x - r, y = center.y - r),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                    style = Stroke(w, cap = StrokeCap.Round),
                )
            }
        }
        val core = Modifier.size(92.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c.accent, c.accentWarm)))
        Box(if (onClick != null) core.pressable(onClick = onClick) else core, contentAlignment = Alignment.Center) {
            NixIconView(icon, c.onAccent, size = 36.dp, strokeWidth = 2.2f)
        }
    }
}

@Composable
private fun ReviewList(review: VoiceStage.Review, vm: VoiceNoteViewModel, s: Strings) {
    val c = Nix.colors
    val editing = review.editing?.let { review.items.getOrNull(it) }
    if (editing != null) {
        VoiceItemEditor(editing.action, vm::problemOf, vm::onSaveEdit, vm::onCancelEdit, s)
        return
    }
    Text(if (review.transcript.isBlank()) s.voiceSuggestTitle else s.voiceReviewTitle, style = NixType.Heading, color = c.textPrimary)
    if (review.transcript.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text("“${review.transcript}”", style = NixType.Small, color = c.textMuted, textAlign = TextAlign.Center, maxLines = 4)
    }
    Spacer(Modifier.height(14.dp))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        review.items.forEachIndexed { index, item ->
            val (icon, kind, title, detail) = describe(item.action, s)
            NixCard(
                Modifier.fillMaxWidth(),
                onClick = { vm.onToggle(index) },
                color = if (item.checked) c.accent.copy(alpha = 0.10f) else c.surfaceRaised.copy(alpha = 0.5f),
                borderColor = if (item.checked) c.accent.copy(alpha = 0.45f) else c.border,
                padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                shapeRadius = 18.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        NixIconView(icon, c.accent, size = 20.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(kind.uppercase(), style = NixType.Eyebrow, color = c.accentWarm)
                        Text(title, style = NixType.BodyStrong, color = if (item.checked) c.textPrimary else c.textMuted)
                        Text(detail, style = NixType.Small, color = c.textSecondary)
                        if (item.problem != null) {
                            Text(s.voiceNeedsFix, style = NixType.SmallStrong, color = c.accentWarm)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    NixIconButton(
                        NixIcon.Pencil,
                        { vm.onEdit(index) },
                        Modifier.semantics { contentDescription = s.voiceEditItem },
                        tint = c.textSecondary,
                        background = c.surfaceRaised,
                        size = 34.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(if (item.checked) c.accent else c.surfaceRaised),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item.checked) NixIconView(NixIcon.Tick, c.onAccent, size = 16.dp, strokeWidth = 2.4f)
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    val chosen = review.items.count { it.checked }
    NixButton(s.voiceAdd(chosen), vm::onConfirm, Modifier.fillMaxWidth(), icon = NixIcon.Plus, enabled = chosen > 0, loading = review.adding)
    Spacer(Modifier.height(6.dp))
    Text(s.voiceReviewHint, style = NixType.Small, color = c.textMuted, textAlign = TextAlign.Center)
}

private data class ActionText(val icon: NixIcon, val kind: String, val title: String, val detail: String)

private fun describe(action: CoachAction, s: Strings): ActionText = when (action) {
    is CoachAction.Quit -> ActionText(NixIcon.Shield, s.actionQuit, action.name, s.actionQuitDetail)
    is CoachAction.Build -> ActionText(
        NixIcon.CircleCheck, s.actionBuild, action.name,
        action.daysPerWeek?.let { s.actionDaysAWeek(it) } ?: s.actionTimesADay(action.perDay),
    )
    is CoachAction.Event -> {
        val date = formatDate(CivilCalendar.dayIndexFromCivil(action.year, action.month, action.day), s, includeWeekday = true)
        val time = action.hour?.let { formatClockMinutes(it * 60 + (action.minute ?: 0), s) }
        val category = if (action.category == EventCategory.EVENT) "" else " · ${s.categoryLabel(action.category)}"
        ActionText(NixIcon.Calendar, s.actionEvent, action.title, (if (time != null) "$date · $time" else date) + category)
    }
}

/** The entry card on Home: one tap to say what you want to add. */
@Composable
fun VoiceCard(onClick: () -> Unit) {
    val c = Nix.colors
    val s = LocalStrings.current
    NixCard(
        Modifier.fillMaxWidth(),
        onClick = onClick,
        color = c.accent.copy(alpha = 0.10f),
        borderColor = c.accent.copy(alpha = 0.30f),
        padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c.accent, c.accentWarm))),
                contentAlignment = Alignment.Center,
            ) {
                NixIconView(NixIcon.Mic, c.onAccent, size = 22.dp, strokeWidth = 2f)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(s.voiceCardTitle, style = NixType.BodyStrong, color = c.textPrimary)
                Text(s.voiceCardBody, style = NixType.Small, color = c.textSecondary)
            }
        }
    }
}
