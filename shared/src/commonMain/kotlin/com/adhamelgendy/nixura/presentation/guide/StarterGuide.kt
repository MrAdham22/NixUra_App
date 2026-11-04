package com.adhamelgendy.nixura.presentation.guide

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.presentation.components.NixButton
import com.adhamelgendy.nixura.presentation.components.NixButtonStyle
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixTextField
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.navigation.Route
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.strings.Strings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType

/** Real parts of the app that the starter guide can point at. */
enum class GuideTarget { Progress, AddQuit, AddGood, AddEvent, NavBar, Avatar }

/** Where each target is on screen. Only provided while the guide runs, so it costs nothing otherwise. */
@Stable
class GuideTargets {
    val bounds = mutableStateMapOf<GuideTarget, Rect>()
}

val LocalGuideTargets = compositionLocalOf<GuideTargets?> { null }

/** Marks this element as something the starter guide can spotlight. */
@Composable
fun Modifier.guideTarget(target: GuideTarget): Modifier {
    val targets = LocalGuideTargets.current ?: return this
    return this.onGloballyPositioned { targets.bounds[target] = it.boundsInRoot() }
}

private data class GuideStep(val title: String, val body: String, val target: GuideTarget? = null, val tab: Route.Tab? = null)

private fun guideSteps(s: Strings) = listOf(
    GuideStep(s.guideWelcomeTitle, s.guideWelcomeBody, tab = Route.Home),
    GuideStep(s.guideScoresTitle, s.guideScoresBody, GuideTarget.Progress, Route.Home),
    GuideStep(s.guideQuitTitle, s.guideQuitBody, GuideTarget.AddQuit, Route.BadHabits),
    GuideStep(s.guideBuildTitle, s.guideBuildBody, GuideTarget.AddGood, Route.GoodHabits),
    GuideStep(s.guideEventsTitle, s.guideEventsBody, GuideTarget.AddEvent, Route.Events),
    GuideStep(s.guideBarTitle, s.guideBarBody, GuideTarget.NavBar, Route.Home),
    GuideStep(s.guideAccountTitle, s.guideAccountBody, GuideTarget.Avatar, Route.Home),
    GuideStep(s.guideDoneTitle, s.guideDoneBody, tab = Route.Home),
)

/**
 * Interactive tour over the real app: it moves through the actual tabs and spotlights the actual controls. Tapping the
 * spotlight moves on, like Next. It ends with one real action: adding a first habit.
 */
@Composable
fun StarterGuide(
    navigator: Navigator,
    currentName: String,
    targets: GuideTargets,
    onSaveName: (String) -> Unit,
    onFinish: () -> Unit,
) {
    val s = LocalStrings.current
    val density = LocalDensity.current
    val steps = remember(s) { guideSteps(s) }
    var index by rememberSaveable { mutableStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val step = steps[index]
    val askName = currentName.isBlank() // read live: the profile is still loading on the first frame

    LaunchedEffect(index) { step.tab?.let(navigator::selectTab) }

    fun finish(firstHabit: Boolean) {
        if (name.isNotBlank()) onSaveName(name.trim())
        onFinish()
        navigator.selectTab(Route.Home)
        if (firstHabit) navigator.push(Route.BadHabitForm())
    }

    fun next() {
        if (index < steps.lastIndex) index += 1
    }

    val margin = with(density) { 8.dp.toPx() }
    val hole = step.target?.let { targets.bounds[it] }?.translate(-origin)?.inflate(margin)
    val center = Offset(size.width / 2f, size.height / 2f)
    val shownHole by animateRectAsState(hole ?: Rect(center, 0f), tween(380), label = "guideHole")
    val radius = with(density) { 22.dp.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                origin = it.positionInRoot()
                size = it.size
            }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawBehind {
                drawRect(Color(0xE0100E24))
                if (shownHole.width > 0f) {
                    drawRoundRect(Color.Black, shownHole.topLeft, shownHole.size, CornerRadius(radius), blendMode = BlendMode.Clear)
                }
            }
            .pointerInput(index, hole) {
                detectTapGestures { tap -> if (hole?.contains(tap) == true) next() }
            },
    ) {
        val pad = 16.dp
        val cardModifier = when {
            hole == null -> Modifier.align(Alignment.Center).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = pad)
            hole.center.y > size.height / 2f -> {
                val fromBottom = with(density) { (size.height - hole.top).toDp() }
                Modifier.align(Alignment.BottomCenter).padding(start = pad, end = pad, bottom = fromBottom + 12.dp)
            }
            else -> {
                val fromTop = with(density) { hole.bottom.toDp() }
                Modifier.align(Alignment.TopCenter).padding(start = pad, end = pad, top = fromTop + 12.dp)
            }
        }
        AnimatedContent(
            targetState = index,
            modifier = cardModifier.fillMaxWidth(),
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "guideCard",
        ) { shown ->
            val current = steps[shown]
            GuideCard(current, shown, steps.size) {
                when (shown) {
                    0 -> {
                        if (askName) {
                            NixTextField(name, { name = it }, label = s.nameLabel, placeholder = s.namePlaceholder, maxLength = 40)
                            Spacer(Modifier.height(14.dp))
                        }
                        NixButton(s.guideStart, ::next, Modifier.fillMaxWidth())
                        NixButton(s.guideSkip, { finish(firstHabit = false) }, Modifier.fillMaxWidth(), style = NixButtonStyle.Ghost)
                    }
                    steps.lastIndex -> {
                        NixButton(s.guideAddFirst, { finish(firstHabit = true) }, Modifier.fillMaxWidth())
                        NixButton(s.guideExplore, { finish(firstHabit = false) }, Modifier.fillMaxWidth(), style = NixButtonStyle.Ghost)
                    }
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NixButton(s.guideSkip, { finish(firstHabit = false) }, Modifier.weight(1f), style = NixButtonStyle.Secondary)
                        NixButton(s.guideNext, ::next, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideCard(step: GuideStep, index: Int, count: Int, actions: @Composable () -> Unit) {
    val c = Nix.colors
    NixCard(Modifier.fillMaxWidth(), color = c.surfaceRaised) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(count) { dot ->
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (dot == index) c.accentWarm else c.textMuted.copy(alpha = 0.35f)))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(step.title, style = NixType.Heading, color = c.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(step.body, style = NixType.Body, color = c.textSecondary)
        Spacer(Modifier.height(16.dp))
        actions()
    }
}
