package com.adhamelgendy.nixura.presentation.components.themeanim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.adhamelgendy.nixura.presentation.appearance.HabitThemeAnimationKind
import com.adhamelgendy.nixura.presentation.platform.rememberReducedMotion
import com.adhamelgendy.nixura.presentation.theme.NixColors
import kotlin.math.abs

/**
 * What a theme scene gets each time it is shown. Scenes draw behind the page; they never take input themselves.
 */
@Immutable
class ThemeSceneParams(
    /** False once the name moved to another theme: the scene plays its exit (water drains) and then calls [onGone]. */
    val active: Boolean,
    /** "Remove animations" is on: draw one still frame, no loops. */
    val reducedMotion: Boolean,
    /** The page is on screen and the app in front: loops run only while true. */
    val running: Boolean,
    val rtl: Boolean,
    /** The theme's accent from [com.adhamelgendy.nixura.presentation.appearance.HabitThemes.visualFor]. */
    val accent: androidx.compose.ui.graphics.Color,
    val colors: NixColors,
    /** For water: how full the page ends up (1 = full; less when editing reflects today's progress). */
    val fillLevel: Float,
    /** The page extends this far under the system bars, in px, so a scene can paint the whole screen. */
    val bleed: Rect,
    /** Taps on the page (seen, never consumed), in this canvas's coordinates. */
    val taps: ThemeTaps,
    val onGone: () -> Unit,
)

/** One theme's background. Each scene is one file; register it in [sceneFor]. */
interface ThemeScene {
    @Composable
    fun Content(params: ThemeSceneParams)
}

/** The single mapping from [HabitThemeAnimationKind] to its scene. A new theme = one new file + one line here. */
internal fun sceneFor(kind: HabitThemeAnimationKind): ThemeScene? = when (kind) {
    HabitThemeAnimationKind.WATER_FILL -> WaterScene
    HabitThemeAnimationKind.GOLDEN_RAYS -> GoldenRaysScene
    HabitThemeAnimationKind.FOOTPRINTS -> FootprintsScene
    HabitThemeAnimationKind.ENERGY_PARTICLES -> EnergyScene
    HabitThemeAnimationKind.FLUTTERING_PAGES -> PagesScene
    HabitThemeAnimationKind.NIGHT_SKY -> NightSkyScene
    HabitThemeAnimationKind.NONE -> null
}

/** Taps seen on the page, kept for the ripples. Capped: only the newest [MAX] are kept. */
class ThemeTaps {
    /** Newest last: where the tap was and its sequence number (scenes stamp their own time on new numbers). */
    internal val items = mutableStateListOf<Pair<Offset, Long>>()
    private var next = 0L

    internal fun add(at: Offset) {
        if (items.size >= MAX) items.removeAt(0)
        items.add(at to next++)
    }

    private companion object {
        const val MAX = 4
    }
}

/**
 * Plays the background of a habit theme behind [content]: [kind] comes from
 * `HabitThemes.visualFor(theme, colors).animation`, never from the name. When [kind] changes, the old scene plays its
 * exit while the new one starts. Taps are only observed (never consumed), so inputs, buttons and the keyboard keep
 * working exactly as without it.
 */
@Composable
fun HabitThemeBackdrop(
    kind: HabitThemeAnimationKind,
    accent: androidx.compose.ui.graphics.Color,
    colors: NixColors,
    modifier: Modifier = Modifier,
    fillLevel: Float = 1f,
    /** Previews and screenshots: draw the still version, as with "remove animations". */
    still: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val reduced = still || rememberReducedMotion()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val running = !reduced && lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing
    val bleed = Rect(
        left = insets.getLeft(density, direction).toFloat(),
        top = insets.getTop(density).toFloat(),
        right = insets.getRight(density, direction).toFloat(),
        bottom = insets.getBottom(density).toFloat(),
    )
    val taps = remember { ThemeTaps() }
    // Scenes on screen: the current one plus any still playing their exit.
    val shown = remember { mutableStateListOf<HabitThemeAnimationKind>() }
    LaunchedEffect(kind) {
        if (kind != HabitThemeAnimationKind.NONE && kind !in shown) shown.add(kind)
    }

    Box(
        modifier.pointerInput(Unit) {
            // Initial pass, nothing consumed: the page's own controls get every event exactly as before.
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val start = down.position
                var moved = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (abs(change.position.x - start.x) + abs(change.position.y - start.y) > viewConfiguration.touchSlop) moved = true
                    if (!change.pressed) break
                }
                if (!moved) taps.add(start)
            }
        },
    ) {
        shown.toList().forEach { k ->
            key(k) {
                sceneFor(k)?.Content(
                    ThemeSceneParams(
                        active = k == kind,
                        reducedMotion = reduced,
                        running = running,
                        rtl = rtl,
                        accent = accent,
                        colors = colors,
                        fillLevel = fillLevel,
                        bleed = bleed,
                        taps = taps,
                        onGone = { shown.remove(k) },
                    ),
                )
            }
        }
        content()
    }
}

/**
 * Seconds since the scene appeared, advanced once per frame only while [running]. Read it only inside drawing, so the
 * scene redraws each frame without recomposing anything. Paused (frozen) when not running.
 */
@Composable
internal fun rememberSceneTime(running: Boolean, stillAt: Float = 3f): State<Float> {
    val time = remember { mutableFloatStateOf(if (running) 0f else stillAt) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = -1L
        while (true) {
            withFrameNanos { now ->
                if (last >= 0) time.floatValue += ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
            }
        }
    }
    return time
}

/** 0..1 fade of a simple scene: in when active, out when not, then [ThemeSceneParams.onGone]. */
@Composable
internal fun rememberPresence(params: ThemeSceneParams, inMs: Int = 700, outMs: Int = 450): State<Float> {
    val presence = remember { Animatable(0f) }
    LaunchedEffect(params.active, params.reducedMotion) {
        if (params.active) {
            if (params.reducedMotion) presence.snapTo(1f) else presence.animateTo(1f, tween(inMs))
        } else {
            if (params.reducedMotion) presence.snapTo(0f) else presence.animateTo(0f, tween(outMs))
            params.onGone()
        }
    }
    return presence.asState()
}
