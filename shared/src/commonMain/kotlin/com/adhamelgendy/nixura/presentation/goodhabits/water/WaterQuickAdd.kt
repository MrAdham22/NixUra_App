package com.adhamelgendy.nixura.presentation.goodhabits.water

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.domain.theme.WaterGoal
import com.adhamelgendy.nixura.presentation.appearance.CardInkLight
import com.adhamelgendy.nixura.presentation.components.GoodFill
import com.adhamelgendy.nixura.presentation.components.cardGlow
import com.adhamelgendy.nixura.presentation.components.pressable
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import kotlinx.coroutines.delay

/** How long "Undo" stays after a quick add. */
private const val UNDO_WINDOW_MS = 5_000L

/**
 * The last quick add, so it can be taken back once. Each add restarts the window; undo takes back one serving
 * (the caller decrements with `onChangeToday(id, -1)`), then hides.
 */
@Stable
class WaterUndoState {
    var token by mutableIntStateOf(0)
        private set
    val visible: Boolean get() = token != 0

    fun added() { token = if (token == Int.MAX_VALUE) 1 else token + 1 }
    fun clear() { token = 0 }
}

@Composable
fun rememberWaterUndo(): WaterUndoState {
    val state = remember { WaterUndoState() }
    LaunchedEffect(state.token) {
        if (state.token != 0) {
            delay(UNDO_WINDOW_MS)
            state.clear()
        }
    }
    return state
}

/**
 * Quick add of one serving: "+1 cup" or "+250 ml". Same recipe as the water card's round "+": the rose action while
 * the glass is empty, turning blue as it fills.
 */
@Composable
fun WaterAddButton(goal: WaterGoal, look: GoodFill, onAdd: () -> Unit, modifier: Modifier = Modifier, height: Dp = 44.dp) {
    val s = LocalStrings.current
    val c = Nix.colors
    val t = (look.fill / 0.4f).coerceIn(0f, 1f)
    val rose = listOf(c.accent, lerp(c.accent, c.accentWarm, 0.45f))
    val blue = listOf(lerp(c.water, c.waterDeep, 0.45f), lerp(c.water, Color.White, 0.18f))
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .pressable(pressedScale = 0.92f, haptic = true, onClick = onAdd)
            .heightIn(min = height)
            .cardGlow(shape, listOf(lerp(rose[0], blue[0], t), lerp(rose[1], blue[1], t)))
            .semantics { contentDescription = s.waterAddA11y(goal.servingMl) }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(WaterProgress.quickAddLabel(goal, s), style = NixType.SmallStrong, color = CardInkLight, maxLines = 1)
    }
}

/** The progress line ("1.2 / 2 L") with a small "Undo" beside it for a few seconds after a quick add. */
@Composable
fun WaterProgressLine(
    text: String,
    undo: WaterUndoState,
    onUndo: () -> Unit,
    color: Color,
    undoColor: Color,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = NixType.Small, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        AnimatedVisibility(undo.visible, enter = fadeIn(), exit = fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(10.dp))
                Text(
                    s.waterUndo,
                    style = NixType.SmallStrong.copy(textDecoration = TextDecoration.Underline),
                    color = undoColor,
                    modifier = Modifier
                        .pressable(haptic = true) { undo.clear(); onUndo() }
                        .semantics { contentDescription = s.waterUndoA11y }
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}
