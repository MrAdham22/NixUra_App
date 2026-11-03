package com.example.reclaimapp.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.reclaimapp.domain.calculator.HabitScoreLine
import com.example.reclaimapp.presentation.profile.ProfileStats
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType

enum class ScoreKind { BreakingFree, Discipline, Momentum }

/** Short "what is this number" sheet for any tile or streak: the value, then one plain explanation. */
@Composable
fun InfoSheet(title: String, value: String?, body: String, onDismiss: () -> Unit) {
    val c = Nix.colors
    NixSheet(onDismiss) {
        Text(title, style = NixType.Heading, color = c.textPrimary)
        if (value != null) {
            Spacer(Modifier.height(6.dp))
            Text(value, style = NixType.counter(36), color = c.accentWarm)
        }
        Spacer(Modifier.height(10.dp))
        Text(body, style = NixType.Body, color = c.textSecondary)
    }
}

/** Shows how a percentage was reached: the score, the rule behind it, and every habit's share. */
@Composable
fun ScoreSheet(kind: ScoreKind, stats: ProfileStats, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val c = Nix.colors
    val title = when (kind) {
        ScoreKind.BreakingFree -> s.quitScoreTitle
        ScoreKind.Discipline -> s.disciplineScoreTitle
        ScoreKind.Momentum -> s.momentum
    }
    val score = when (kind) {
        ScoreKind.BreakingFree -> stats.quitScore
        ScoreKind.Discipline -> stats.disciplineScore
        ScoreKind.Momentum -> stats.momentum
    }
    val color = when (kind) {
        ScoreKind.BreakingFree -> c.accent
        ScoreKind.Discipline -> c.positive
        ScoreKind.Momentum -> c.accentWarm
    }
    val rule = when (kind) {
        ScoreKind.BreakingFree -> s.scoreRuleBreakingFree
        ScoreKind.Discipline -> s.scoreRuleDiscipline
        ScoreKind.Momentum -> s.scoreRuleMomentum
    }
    val tip = when (kind) {
        ScoreKind.BreakingFree -> s.scoreTipBreakingFree
        ScoreKind.Discipline -> s.scoreTipDiscipline
        ScoreKind.Momentum -> s.scoreTipMomentum
    }
    NixSheet(onDismiss) {
        Text(title, style = NixType.Heading, color = c.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(score?.let { "$it%" } ?: "—", style = NixType.counter(44), color = color)
        Spacer(Modifier.height(10.dp))
        Text(s.scoreHowTitle.uppercase(), style = NixType.Eyebrow, color = c.textMuted)
        Spacer(Modifier.height(6.dp))
        Text(rule, style = NixType.Body, color = c.textSecondary)
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (kind) {
                ScoreKind.BreakingFree -> stats.quitLines.forEach { ScoreLine(it, color) }
                ScoreKind.Discipline -> stats.disciplineLines.forEach { ScoreLine(it, color) }
                ScoreKind.Momentum -> {
                    stats.quitScore?.let { ScoreLine(HabitScoreLine(s.quitScoreTitle, it), c.accent) }
                    stats.disciplineScore?.let { ScoreLine(HabitScoreLine(s.disciplineScoreTitle, it), c.positive) }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(tip, style = NixType.Small, color = c.textMuted)
    }
}

@Composable
private fun ScoreLine(line: HabitScoreLine, color: Color) {
    val c = Nix.colors
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(line.name, style = NixType.BodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Text("${line.percent}%", style = NixType.BodyStrong, color = c.textSecondary)
        }
        Spacer(Modifier.height(6.dp))
        NixProgressBar(line.percent / 100f, color = color, height = 6.dp)
    }
}
