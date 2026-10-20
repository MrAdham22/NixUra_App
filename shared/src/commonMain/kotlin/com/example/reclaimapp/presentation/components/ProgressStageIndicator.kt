package com.example.reclaimapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ProgressStageIndicator(
    stageCount: Int,
    activeStageIndex: Int,
    modifier: Modifier = Modifier,
    labelColor: Color,
    activeStageColor: Color,
    inactiveStageColor: Color,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(stageCount) { index ->
            val active = index <= activeStageIndex
            Box(
                modifier = Modifier
                    .size(if (index == activeStageIndex) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (active) activeStageColor else inactiveStageColor),
            )
        }
        Text(
            text = "Stage ${activeStageIndex + 1}/$stageCount",
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
        )
    }
}
