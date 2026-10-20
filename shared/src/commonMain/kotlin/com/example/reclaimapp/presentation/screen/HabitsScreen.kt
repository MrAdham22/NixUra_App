package com.example.reclaimapp.presentation.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.presentation.appearance.StreakColorMapper
import com.example.reclaimapp.presentation.components.HabitCard
import com.example.reclaimapp.presentation.habits.HabitsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HabitsScreen(
    modifier: Modifier = Modifier,
    viewModel: HabitsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mapper = StreakColorMapper()

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
            text = "Your habits — offline tracking with streak progress.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = viewModel::onDismissError) {
                Text("Dismiss")
            }
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.habits.isEmpty() -> {
                Text(
                    text = "No habits yet. Add one to start tracking.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                uiState.habits.forEach { habit ->
                    val cardColor by animateColorAsState(mapper.cardBackgroundForActiveStreak(habit.activeElapsed))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HabitCard(
                            habitName = habit.name,
                            activeElapsed = habit.activeElapsed,
                            longestStreakLabel = habit.longestStreakLabel,
                            cardBackground = cardColor,
                            activeStageIndex = habit.activeStageIndex,
                            stageCount = habit.stageCount,
                        )
                        OutlinedButton(
                            onClick = { viewModel.onRecordRelapse(habit.habitId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Record relapse")
                        }
                    }
                }
            }
        }

        Button(
            onClick = viewModel::onAddHabitClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
            ),
        ) {
            Text("Add habit")
        }
    }

    if (uiState.showCreateDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissCreateDialog,
            title = { Text("New habit") },
            text = {
                OutlinedTextField(
                    value = uiState.createNameInput,
                    onValueChange = viewModel::onCreateNameChange,
                    label = { Text("Habit name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onConfirmCreateHabit,
                    enabled = !uiState.isSaving,
                ) {
                    Text(if (uiState.isSaving) "Saving…" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissCreateDialog) {
                    Text("Cancel")
                }
            },
        )
    }
}
