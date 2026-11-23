package com.adhamelgendy.nixura.presentation.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.MotionDurationScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlin.coroutines.coroutineContext

/**
 * Whether looping animations may run here. False while the app is in the background and on tab pages that are not on
 * screen (the pager keeps its neighbours composed), so the app draws nothing nobody sees. Loops freeze in place and
 * carry on from the same point when this turns true again, so the look never changes: only the hidden frames go.
 */
val LocalAnimationsRunning = compositionLocalOf { true }

/** Turns looping animations off while the app is not visible (below STARTED). Wraps the whole app. */
@Composable
fun AnimationsWhileVisible(content: @Composable () -> Unit) {
    val state by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val visible = state.isAtLeast(Lifecycle.State.STARTED)
    CompositionLocalProvider(LocalAnimationsRunning provides (LocalAnimationsRunning.current && visible), content = content)
}

/**
 * A value looping between [from] and [to], like `rememberInfiniteTransition().animateFloat(from, to,
 * infiniteRepeatable(tween(durationMillis, delayMillis, easing), if (reverse) Reverse else Restart))`, but it pauses
 * while [LocalAnimationsRunning] is false. Read it only while drawing (in `drawBehind`, `drawWithCache`, a Canvas or
 * `graphicsLayer`), so each frame repaints without recomposing.
 */
@Composable
fun rememberLoop(
    durationMillis: Int,
    from: Float = 0f,
    to: Float = 1f,
    easing: Easing = LinearEasing,
    delayMillis: Int = 0,
    reverse: Boolean = false,
): State<Float> {
    val running = LocalAnimationsRunning.current
    val elapsed = remember { longArrayOf(0L) }
    val value = remember { mutableFloatStateOf(from) }
    LaunchedEffect(running, durationMillis, delayMillis, reverse, from, to, easing) {
        fun publish() {
            val f = LoopMath.fraction(elapsed[0], durationMillis, delayMillis, reverse)
            value.floatValue = from + (to - from) * easing.transform(f)
        }
        publish()
        if (!running) return@LaunchedEffect
        // Follow the system animation speed (Developer options), as Compose's own animations do.
        val scale = coroutineContext[MotionDurationScale]?.scaleFactor?.takeIf { it > 0f } ?: 1f
        var last = -1L
        var lastSlot = -1L
        while (true) {
            withFrameMillis { now ->
                // A long gap (the frame clock paused) must not jump the loop ahead.
                if (last >= 0) elapsed[0] += ((now - last).coerceAtMost(MAX_FRAME_STEP_MILLIS) / scale).toLong()
                last = now
                // Ambient loops move at up to 60 steps a second, on one shared clock so they all step on the same
                // frames: on a 120 Hz screen that halves their redraws, and a slow drift looks the same. Touch
                // feedback and screen changes are not loops and keep the screen's full rate.
                val slot = now / AMBIENT_FRAME_MILLIS
                if (slot != lastSlot) {
                    lastSlot = slot
                    publish()
                }
            }
        }
    }
    return value
}

private const val MAX_FRAME_STEP_MILLIS = 100L

/** One step of an ambient loop: 16 ms, about 60 a second. */
const val AMBIENT_FRAME_MILLIS = 16L

/** Where a looping tween is after a play time, before easing. */
object LoopMath {
    /**
     * 0..1 progress after [elapsedMillis] of a loop whose every pass waits [delayMillis] and then runs for
     * [durationMillis]. With [reverse], every second pass runs back from 1 to 0.
     */
    fun fraction(elapsedMillis: Long, durationMillis: Int, delayMillis: Int = 0, reverse: Boolean = false): Float {
        if (durationMillis <= 0) return 1f
        val cycle = (durationMillis + delayMillis.coerceAtLeast(0)).toLong()
        val pass = elapsedMillis.coerceAtLeast(0) / cycle
        val inPass = elapsedMillis.coerceAtLeast(0) % cycle
        val f = ((inPass - delayMillis).coerceAtLeast(0).toFloat() / durationMillis).coerceIn(0f, 1f)
        return if (reverse && pass % 2 == 1L) 1f - f else f
    }
}
