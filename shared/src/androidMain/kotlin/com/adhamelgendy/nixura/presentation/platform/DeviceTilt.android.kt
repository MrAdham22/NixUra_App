package com.adhamelgendy.nixura.presentation.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

@Composable
actual fun rememberDeviceTilt(enabled: Boolean): State<Float> {
    val tilt = remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val listen = enabled && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    DisposableEffect(listen, context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!listen || manager == null || sensor == null) {
            tilt.floatValue = 0f
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // x is the sideways gravity in m/s²; tilting the right edge down makes it negative.
                val raw = -event.values[0] / (SensorManager.GRAVITY_EARTH * 0.7071f)
                tilt.floatValue = smoothTilt(tilt.floatValue, raw)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        // SENSOR_DELAY_UI (~60 ms) is plenty for a slow slosh and far cheaper than the game rate.
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    return tilt
}
