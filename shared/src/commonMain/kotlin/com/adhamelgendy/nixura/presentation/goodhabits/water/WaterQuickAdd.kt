package com.adhamelgendy.nixura.presentation.goodhabits.water

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.adhamelgendy.nixura.presentation.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

/**
 * Quick add of one serving: "+1 cup" or "+250 ml". Same recipe as the water card's round "+": the rose action while
 * the glass is empty, turning blue as it fills. The card's "−" takes a serving back.
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
