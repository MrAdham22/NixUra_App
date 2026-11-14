package com.adhamelgendy.nixura.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.presentation.theme.NixType

/**
 * The streak's color stages as dots (reached ones filled, the current one larger, the rest outlined so they still
 * read on any card color) followed by [label], usually the next milestone and the time left to it.
 */
@Composable
fun ProgressStageIndicator(
    stageCount: Int,
    activeStageIndex: Int,
    label: String,
    modifier: Modifier = Modifier,
    labelColor: Color,
    activeStageColor: Color,
    inactiveStageColor: Color,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(stageCount) { index ->
                val reached = index <= activeStageIndex
                Box(
                    modifier = Modifier
                        .size(if (index == activeStageIndex) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .then(
                            if (reached) Modifier.background(activeStageColor)
                            else Modifier.border(1.5.dp, inactiveStageColor, CircleShape),
                        ),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(text = label, style = NixType.SmallStrong, color = labelColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
