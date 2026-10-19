package com.example.reclaimapp.presentation.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.reclaimapp.presentation.appearance.StreakColorProgressionConfig
import com.example.reclaimapp.presentation.appearance.StreakColorMapper
import com.example.reclaimapp.presentation.components.HabitCard
import com.example.reclaimapp.presentation.util.formatElapsed
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

/**
 * Phase 3 representative screen: app-wide NixUra theme + habit card with streak-based color progression.
 * Uses simulated elapsed time (not persistence). Longest streak is demo-only text.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignPreviewScreen(modifier: Modifier = Modifier) {
    val mapper = remember { StreakColorMapper() }
    var activeElapsedMillis by remember { mutableLongStateOf(0L) }
    var longestElapsedMillis by remember { mutableLongStateOf(12.days.inWholeMilliseconds) }
    var liveTick by remember { mutableStateOf(false) }

    LaunchedEffect(liveTick) {
        if (!liveTick) return@LaunchedEffect
        while (liveTick) {
            kotlinx.coroutines.delay(1_000)
            activeElapsedMillis += 1_000
            if (activeElapsedMillis > longestElapsedMillis) {
                longestElapsedMillis = activeElapsedMillis
            }
        }
    }

    val activeElapsed = activeElapsedMillis.milliseconds
    val cardColor by animateColorAsState(mapper.cardBackgroundForActiveStreak(activeElapsed))
    val stageIndex = mapper.stageIndexFor(activeElapsed)
    val stageCount = StreakColorProgressionConfig.Default.stopCount

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "NixUra",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Design preview — theme tokens and streak-based habit card (provisional thresholds: ${mapper.configLabel()}).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HabitCard(
            habitName = "Social scrolling",
            activeElapsed = activeElapsed,
            longestStreakLabel = formatElapsed(longestElapsedMillis.milliseconds),
            cardBackground = cardColor,
            activeStageIndex = stageIndex,
            stageCount = stageCount,
        )

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Simulate active streak", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "Start" to Duration.ZERO,
                        "12h" to 12.hours,
                        "1d" to 1.days,
                        "3d" to 3.days,
                        "7d" to 7.days,
                        "14d" to 14.days,
                        "30d+" to 30.days,
                    ).forEach { (label, duration) ->
                        OutlinedButton(onClick = {
                            activeElapsedMillis = duration.inWholeMilliseconds
                            liveTick = false
                        }) {
                            Text(label)
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { liveTick = !liveTick },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                        ),
                    ) {
                        Text(if (liveTick) "Pause live timer" else "Live timer (+1s)")
                    }
                    Button(
                        onClick = {
                            if (activeElapsedMillis > longestElapsedMillis) {
                                longestElapsedMillis = activeElapsedMillis
                            }
                            activeElapsedMillis = 0L
                            liveTick = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Record relapse (demo)")
                    }
                }
                Text(
                    text = "Relapse resets the card to the dark starting color. Longest streak label is unchanged in this demo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
