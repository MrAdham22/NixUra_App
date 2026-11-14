package com.adhamelgendy.nixura.presentation.coach

import com.adhamelgendy.nixura.presentation.components.ringSweep
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.domain.coach.ai.CoachVisual
import com.adhamelgendy.nixura.domain.coach.durationText
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.platform.rememberReducedMotion
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/** One progress card under a coach reply. It draws itself in: the ring sweeps, the bars grow, the numbers count up. */
@Composable
fun CoachVisualCard(visual: CoachVisual, modifier: Modifier = Modifier) {
    val c = Nix.colors
    NixCard(
        modifier.widthIn(max = 320.dp).fillMaxWidth(),
        color = c.surface,
        borderColor = c.accent.copy(alpha = 0.35f),
        padding = PaddingValues(16.dp),
        shapeRadius = 20.dp,
    ) {
        when (visual) {
            is CoachVisual.Quit -> QuitVisual(visual)
            is CoachVisual.Good -> GoodVisual(visual)
            is CoachVisual.Overview -> OverviewVisual(visual)
        }
    }
}

/** 0 → 1 once, when the card first appears (or 1 at once when the user asked the system for less motion). */
@Composable
private fun rememberReveal(durationMillis: Int = 900, delayMillis: Int = 0): Float {
    val reduced = rememberReducedMotion()
    val reveal = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) reveal.animateTo(1f, tween(durationMillis, delayMillis, FastOutSlowInEasing))
    }
    return reveal.value
}

@Composable
private fun Ring(progress: Float, size: Dp, stroke: Dp, track: Color, colors: List<Color>, center: @Composable () -> Unit) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = stroke.toPx()
            val inset = Offset(w / 2, w / 2)
            val arc = Size(this.size.width - w, this.size.height - w)
            drawArc(track, -90f, 360f, false, inset, arc, style = Stroke(w))
            val sweep = ringSweep(progress)
            if (sweep > 0f) {
                drawArc(Brush.sweepGradient(colors + colors.first()), -90f, sweep, false, inset, arc, style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        center()
    }
}

@Composable
private fun CardTitle(eyebrow: String, name: String) {
    val c = Nix.colors
    Text(eyebrow.uppercase(), style = NixType.Eyebrow, color = c.accentWarm)
    Text(name, style = NixType.Heading, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun QuitVisual(v: CoachVisual.Quit) {
    val c = Nix.colors
    val s = LocalStrings.current
    val reveal = rememberReveal()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Ring(v.progress * reveal, 92.dp, 10.dp, c.surfaceRaised, listOf(c.accent, c.accentWarm)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(v.progress * reveal * 100).roundToInt()}%", style = NixType.Heading, color = c.textPrimary)
                Text(s.visualToGoal(v.goalDays), style = NixType.Smaller, color = c.textMuted)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            CardTitle(s.visualQuitEyebrow, v.name)
            Spacer(Modifier.height(6.dp))
            Text(durationText((v.elapsedMillis * reveal).toLong().milliseconds), style = NixType.Title, color = c.accentWarm, maxLines = 1)
            Text(s.visualClean, style = NixType.Small, color = c.textSecondary)
        }
    }
    Spacer(Modifier.height(14.dp))
    // This streak against the best one: the bar fills as far as the current streak has come.
    val ofBest = if (v.longestMillis > 0) v.elapsedMillis.toFloat() / v.longestMillis else 1f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(s.visualBest(durationText(v.longestMillis.milliseconds)), style = NixType.Small, color = c.textSecondary, modifier = Modifier.weight(1f))
        Text(s.visualRelapses(v.relapses), style = NixType.Small, color = c.textMuted)
    }
    Spacer(Modifier.height(6.dp))
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.surfaceRaised)) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth((ofBest * reveal).coerceIn(0.02f, 1f)).clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(c.accent, c.accentWarm))),
        )
    }
}

@Composable
private fun GoodVisual(v: CoachVisual.Good) {
    val c = Nix.colors
    val s = LocalStrings.current
    val reduced = rememberReducedMotion()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { CardTitle(s.visualGoodEyebrow, v.name) }
        Column(horizontalAlignment = Alignment.End) {
            Text("${v.count} / ${v.target}", style = NixType.Heading, color = c.accentWarm)
            Text(if (v.weekly) s.visualThisWeek else s.visualToday, style = NixType.Smaller, color = c.textMuted)
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        v.days.forEachIndexed { i, fraction ->
            // Each bar starts a little after the one before it, so the week "plays" from left to right.
            val grow = remember { Animatable(if (reduced) 1f else 0f) }
            LaunchedEffect(Unit) { if (!reduced) grow.animateTo(1f, tween(520, 90 * i, FastOutSlowInEasing)) }
            val today = i == v.days.lastIndex
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxHeight().width(18.dp).clip(RoundedCornerShape(9.dp)).background(c.surfaceRaised))
                    if (fraction > 0f) {
                        Box(
                            Modifier.fillMaxHeight((fraction * grow.value).coerceIn(0.12f, 1f)).width(18.dp).clip(RoundedCornerShape(9.dp))
                                .background(if (fraction >= 1f) Brush.verticalGradient(listOf(c.accentWarm, c.accent)) else Brush.verticalGradient(listOf(c.accent.copy(alpha = 0.55f), c.accent.copy(alpha = 0.55f)))),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(s.weekdayLetters[v.weekdays[i] - 1], style = if (today) NixType.SmallStrong else NixType.Small, color = if (today) c.textPrimary else c.textMuted)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(if (v.weekly) s.visualStreakWeeks(v.streak) else s.visualStreakDays(v.streak), style = NixType.Small, color = c.textSecondary)
}

@Composable
private fun OverviewVisual(v: CoachVisual.Overview) {
    val c = Nix.colors
    val s = LocalStrings.current
    val reveal = rememberReveal()
    Text(s.visualOverviewTitle.uppercase(), style = NixType.Eyebrow, color = c.accentWarm)
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        val percent = v.weekPercent
        if (percent != null) {
            Ring(percent / 100f * reveal, 84.dp, 9.dp, c.surfaceRaised, listOf(c.accent, c.accentWarm)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(percent * reveal).roundToInt()}%", style = NixType.Heading, color = c.textPrimary)
                    Text(s.visualWeek, style = NixType.Smaller, color = c.textMuted)
                }
            }
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (v.quitCount > 0) {
                StatLine(s.visualLongestClean, durationText((v.bestCleanMillis * reveal).toLong().milliseconds))
                StatLine(s.visualQuitting, (v.quitCount * reveal).roundToInt().toString())
            }
            if (v.goodTotal > 0) StatLine(s.visualDoneToday, "${(v.goodDone * reveal).roundToInt()} / ${v.goodTotal}")
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    val c = Nix.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = NixType.Small, color = c.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = NixType.BodyStrong, color = c.textPrimary, maxLines = 1)
    }
}
