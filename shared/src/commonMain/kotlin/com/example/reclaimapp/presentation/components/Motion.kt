package com.example.reclaimapp.presentation.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import com.example.reclaimapp.presentation.platform.rememberReducedMotion

/** The app's one easing curve for moving things: leaves quickly, settles softly. */
val NixEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/**
 * Tappable surface with the app's touch response: it dips and dims under the finger and springs back, instead of a
 * ripple. Put it first in the modifier chain so the whole element moves. Honours "remove animations".
 */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    haptic: Boolean = false,
    role: Role? = Role.Button,
    /** Long press (with a firm haptic), e.g. to open a card's delete option. */
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val feedback = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        if (pressed && !reducedMotion) pressedScale else 1f,
        spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    val dim by animateFloatAsState(if (pressed) 0.82f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "pressDim")
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = dim
        }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onLongClick = onLongClick?.let { long ->
                {
                    feedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    long()
                }
            },
        ) {
            if (haptic) feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}
