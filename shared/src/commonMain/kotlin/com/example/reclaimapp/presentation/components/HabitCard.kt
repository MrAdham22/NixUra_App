package com.example.reclaimapp.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.reclaimapp.presentation.appearance.prefersLightContentOnCard
import com.example.reclaimapp.presentation.theme.NixUraPalette
import kotlin.time.Duration

@Composable
fun HabitCard(
    habitName: String,
    activeElapsed: Duration,
    longestStreakLabel: String,
    cardBackground: Color,
    activeStageIndex: Int,
    stageCount: Int,
    modifier: Modifier = Modifier,
) {
    val lightOnCard = cardBackground.prefersLightContentOnCard()
    val primaryText = if (lightOnCard) NixUraPalette.onDarkPrimary else NixUraPalette.onLightPrimary
    val mutedText = if (lightOnCard) NixUraPalette.onDarkMuted else NixUraPalette.onLightMuted

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.large,
            ),
        shape = MaterialTheme.shapes.large,
        color = cardBackground,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = habitName,
                style = MaterialTheme.typography.titleMedium,
                color = primaryText,
            )
            Text(
                text = "Active streak",
                style = MaterialTheme.typography.labelMedium,
                color = mutedText,
            )
            Text(
                text = com.example.reclaimapp.presentation.util.formatElapsed(activeElapsed),
                style = MaterialTheme.typography.headlineLarge,
                color = primaryText,
            )
            Text(
                text = "Longest: $longestStreakLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = mutedText,
            )
            ProgressStageIndicator(
                stageCount = stageCount,
                activeStageIndex = activeStageIndex,
            )
        }
    }
}
