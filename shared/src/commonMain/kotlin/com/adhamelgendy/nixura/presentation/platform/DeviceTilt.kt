package com.adhamelgendy.nixura.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * The phone's sideways tilt (roll), smoothed, in -1..1 (0 = held upright, ±1 = about 45° to a side; positive = the
 * right edge lower). Reads the accelerometer at a low rate and only while [enabled] and the screen is shown, so it
 * costs almost no battery. Stays 0 where there is no sensor.
 */
@Composable
expect fun rememberDeviceTilt(enabled: Boolean): State<Float>

/** Low-pass filter shared by both platforms: moves [previous] a step towards [raw]. */
internal fun smoothTilt(previous: Float, raw: Float, factor: Float = 0.15f): Float =
    previous + (raw.coerceIn(-1f, 1f) - previous) * factor
