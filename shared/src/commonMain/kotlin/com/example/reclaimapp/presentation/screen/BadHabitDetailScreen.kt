package com.example.reclaimapp.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reclaimapp.domain.calculator.toCounterParts
import com.example.reclaimapp.domain.coach.CoachEngine
import com.example.reclaimapp.domain.coach.durationText
import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.presentation.appearance.StreakColorProgressionConfig
import com.example.reclaimapp.presentation.appearance.prefersLightContentOnCard
import com.example.reclaimapp.presentation.components.CounterBlocks
import com.example.reclaimapp.presentation.components.ConfirmDialog
import com.example.reclaimapp.presentation.components.ErrorBanner
import com.example.reclaimapp.presentation.components.GlyphBadge
import com.example.reclaimapp.presentation.components.AddToHomeScreenButton
import com.example.reclaimapp.domain.widget.WidgetKind
import com.example.reclaimapp.presentation.components.NixButton
import com.example.reclaimapp.presentation.components.NixButtonStyle
import com.example.reclaimapp.presentation.components.NixCard
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.NixIconButton
import com.example.reclaimapp.presentation.components.NixIconView
import com.example.reclaimapp.presentation.components.NixProgressBar
import com.example.reclaimapp.presentation.components.SectionLabel
import com.example.reclaimapp.presentation.components.SubScreen
import com.example.reclaimapp.presentation.components.badGlyphFor
import com.example.reclaimapp.presentation.components.habitCardColor
import com.example.reclaimapp.presentation.habits.HabitsViewModel
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.navigation.Route
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import com.example.reclaimapp.presentation.theme.NixUraPalette
import com.example.reclaimapp.presentation.util.formatDate
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration

@Composable
fun BadHabitDetailScreen(habitId: Long, navigator: Navigator) {
    val vm: HabitsViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val engine = koinInject<CoachEngine>()
    val days = koinInject<DayCalculator>()
    val s = LocalStrings.current
    val c = Nix.colors
    val model = state.habits.firstOrNull { it.habitId == habitId }
    var confirmRelapse by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }

    SubScreen(
        title = model?.name ?: "",
        onBack = { navigator.back() },
        actions = {
            if (model != null) {
                NixIconButton(NixIcon.Pencil, { navigator.push(Route.BadHabitForm(habitId)) }, size = 40.dp)
                Spacer(Modifier.width(8.dp))
                NixIconButton(NixIcon.Trash, { confirmArchive = true }, size = 40.dp, tint = c.danger)
            }
        },
    ) {
        state.errorMessage?.let { ErrorBanner(it, vm::onDismissError) }
        if (model == null) {
            if (!state.isLoading) Text(s.habitNotFound, style = NixType.Body, color = c.textSecondary)
            return@SubScreen
        }

        val background = habitCardColor(model)
        val lightOnCard = background.prefersLightContentOnCard()
        val primary = if (lightOnCard) NixUraPalette.onDarkPrimary else NixUraPalette.onLightPrimary
        val muted = if (lightOnCard) NixUraPalette.onDarkMuted else NixUraPalette.onLightMuted
        val category = remember(model.name) { engine.categoryFor(model.name) }
        val insight = engine.insightFor(model.name, model.activeElapsed)

        // Hero
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(background)
                .border(1.dp, if (lightOnCard) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphBadge(badGlyphFor(model.habitId), primary, primary.copy(alpha = 0.14f), size = 46.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(model.name, style = NixType.Title, color = primary)
                    Text(category.label, style = NixType.Small, color = muted)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.clearFor.uppercase(), style = NixType.Eyebrow, color = muted)
                CounterBlocks(model.activeElapsed.toCounterParts(), primary, muted, valueSize = 44, gap = 18.dp)
            }
            // Progress to the next card shade
            val bounds = StreakColorProgressionConfig.Default.segmentUpperBounds
            val nextBound = bounds.firstOrNull { it > model.activeElapsed }
            val prevBound = bounds.lastOrNull { it <= model.activeElapsed } ?: Duration.ZERO
            if (nextBound != null) {
                val progress = ((model.activeElapsed - prevBound) / (nextBound - prevBound)).toFloat().coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NixProgressBar(progress, color = primary, track = primary.copy(alpha = 0.18f), height = 6.dp)
                    Text(s.nextShadeIn(durationText(nextBound - model.activeElapsed)), style = NixType.Small, color = muted)
                }
            } else {
                Text(s.shadeMax, style = NixType.Small, color = muted)
            }
        }

        AddToHomeScreenButton(WidgetKind.QUIT_COUNTER, model.habitId)

        // Stats
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(s.statCurrent, durationText(model.activeElapsed), Modifier.weight(1f))
            StatTile(s.statBest, durationText(model.longestStreak), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(s.statRelapses, model.relapseCount.toString(), Modifier.weight(1f))
            StatTile(
                s.statTrackingSince,
                model.trackingStartEpochMillis?.let { formatDate(days.dayIndexOf(it), s) } ?: "—",
                Modifier.weight(1f),
            )
        }

        // Coach note


        // Body & mind timeline
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(s.timelineTitle)
            NixCard {
                category.milestones.forEachIndexed { index, milestone ->
                    val achieved = milestone.after <= model.activeElapsed
                    val isNext = insight.next == milestone
                    Row(Modifier.padding(vertical = 8.dp)) {
                        Box(
                            Modifier
                                .padding(top = 3.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (achieved) c.positive else if (isNext) c.accent.copy(alpha = 0.35f) else c.surfaceRaised)
                                .border(1.dp, if (isNext) c.accent else c.border, CircleShape),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(
                                    milestone.title,
                                    style = NixType.BodyStrong,
                                    color = if (achieved) c.textPrimary else c.textSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    if (achieved) s.reached else durationText(milestone.after - model.activeElapsed),
                                    style = NixType.SmallStrong,
                                    color = if (achieved) c.positive else c.textMuted,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(milestone.detail, style = NixType.Small, color = c.textMuted)
                            if (isNext) {
                                Spacer(Modifier.height(8.dp))
                                NixProgressBar(insight.progressToNext, height = 5.dp)
                            }
                        }
                    }
                    if (index != category.milestones.lastIndex) Spacer(Modifier.height(2.dp))
                }
            }
            Text(s.medicalDisclaimer, style = NixType.Smaller, color = c.textMuted)
        }

        // Relapse history
        if (model.relapseTimestamps.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(s.relapseHistory)
                NixCard {
                    model.relapseTimestamps.take(10).forEachIndexed { index, ts ->
                        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            NixIconView(NixIcon.Reset, c.textMuted, size = 16.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(formatDate(days.dayIndexOf(ts), s, includeWeekday = true), style = NixType.Body, color = c.textSecondary)
                        }
                    }
                }
            }
        }

        NixButton(s.recordRelapse, { confirmRelapse = true }, style = NixButtonStyle.Danger, icon = NixIcon.Reset, modifier = Modifier.fillMaxWidth())
        Text(s.relapseHint, style = NixType.Smaller, color = c.textMuted)
    }

    if (confirmRelapse && model != null) {
        ConfirmDialog(
            title = s.relapseConfirmTitle,
            message = s.relapseConfirmBody,
            confirmText = s.recordRelapse,
            cancelText = s.cancel,
            danger = true,
            onConfirm = {
                vm.onRecordRelapse(model.habitId)
                confirmRelapse = false
            },
            onDismiss = { confirmRelapse = false },
        )
    }
    if (confirmArchive && model != null) {
        ConfirmDialog(
            title = s.removeHabitTitle,
            message = s.removeHabitBody,
            confirmText = s.remove,
            cancelText = s.cancel,
            danger = true,
            onConfirm = {
                confirmArchive = false
                vm.onArchiveHabit(model.habitId) { navigator.back() }
            },
            onDismiss = { confirmArchive = false },
        )
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val c = Nix.colors
    NixCard(modifier, padding = PaddingValues(16.dp)) {
        Text(label.uppercase(), style = NixType.Eyebrow, color = c.textMuted, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(value, style = NixType.Heading, color = c.textPrimary, maxLines = 1)
    }
}
