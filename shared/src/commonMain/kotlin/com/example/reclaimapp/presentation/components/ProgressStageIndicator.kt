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
import androidx.compose.ui.unit.dp
import com.example.reclaimapp.presentation.theme.NixUraPalette

@Composable
fun ProgressStageIndicator(
    stageCount: Int,
    activeStageIndex: Int,
    modifier: Modifier = Modifier,
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
                    .background(
                        if (active) NixUraPalette.Orange else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    ),
            )
        }
        Text(
            text = "Stage ${activeStageIndex + 1}/$stageCount",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
