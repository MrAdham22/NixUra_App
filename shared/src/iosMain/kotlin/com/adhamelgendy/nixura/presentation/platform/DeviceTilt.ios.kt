package com.adhamelgendy.nixura.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

// Unverified: iOS is not built on the Windows machine this was written on.
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberDeviceTilt(enabled: Boolean): State<Float> {
    val tilt = remember { mutableFloatStateOf(0f) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val listen = enabled && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    DisposableEffect(listen) {
        val manager = CMMotionManager()
        if (!listen || !manager.accelerometerAvailable) {
            tilt.floatValue = 0f
            return@DisposableEffect onDispose { }
        }
        // 10 Hz: enough for a slow slosh, cheap on the battery.
        manager.accelerometerUpdateInterval = 0.1
        manager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) { data, _ ->
            data?.acceleration?.useContents {
                // x is in g; tilting the right edge down makes it positive on iOS.
                tilt.floatValue = smoothTilt(tilt.floatValue, (x / 0.7071).toFloat())
            }
        }
        onDispose { manager.stopAccelerometerUpdates() }
    }
    return tilt
}
