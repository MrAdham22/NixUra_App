package com.example.reclaimapp.presentation.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.reclaimapp.domain.money.Currencies
import com.example.reclaimapp.domain.money.MoneySavedCalculator
import com.example.reclaimapp.domain.money.StreakSaving
import com.example.reclaimapp.presentation.components.NixButton
import com.example.reclaimapp.presentation.components.NixButtonStyle
import com.example.reclaimapp.presentation.components.NixCard
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.SectionLabel
import com.example.reclaimapp.presentation.habits.HabitCardUiModel
import com.example.reclaimapp.presentation.strings.LocalStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import kotlin.math.roundToLong

private const val MAX_BARS = 8

/** Money-saved analysis for a quit habit: the live total, units not consumed, daily cost and one bar per clean streak. */
@Composable
internal fun MoneySavedSection(model: HabitCardUiModel, nowEpochMillis: Long, onEdit: () -> Unit) {
    val s = LocalStrings.current
    val c = Nix.colors
    val cost = model.cost ?: return
    val saved = model.moneySaved ?: return
    val streaks = MoneySavedCalculator.streaks(cost, model.trackingStartEpochMillis, model.relapseTimestamps, nowEpochMillis)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(s.moneyTitle)
        NixCard {
            Text(s.moneySavedThisStreak.uppercase(), style = NixType.Eyebrow, color = c.textMuted)
            Spacer(Modifier.height(6.dp))
            Text(Currencies.format(saved.savedMinor, saved.currency), style = NixType.counter(34), color = c.positive, maxLines = 1)
            Text(s.unitsAvoided(saved.unitsAvoided, cost.unit), style = NixType.BodyStrong, color = c.textSecondary)
            Spacer(Modifier.height(10.dp))
            val perDay = cost.unitsPerDay.let { if (it == kotlin.math.floor(it)) it.toLong().toString() else ((it * 10).roundToLong() / 10.0).toString() }
            Text(
                "${s.moneyDailyCost}: " + s.moneyDailyCostLine(
                    Currencies.format(MoneySavedCalculator.dailyCostMinor(cost).roundToLong(), cost.currency),
                    "$perDay ${s.costUnitName(cost.unit, cost.unitsPerDay != 1.0)}",
                ),
                style = NixType.Small,
                color = c.textMuted,
            )
            if (cost.item.isNotBlank()) Text(cost.item, style = NixType.Small, color = c.textMuted)

            if (streaks.size > 1) {
                Spacer(Modifier.height(16.dp))
                Text(s.moneyPerStreak, style = NixType.SmallStrong, color = c.textSecondary)
                Spacer(Modifier.height(8.dp))
                StreakBars(streaks.takeLast(MAX_BARS), cost.currency)
            }
            Spacer(Modifier.height(14.dp))
            NixButton(s.moneyEdit, onEdit, Modifier.fillMaxWidth(), style = NixButtonStyle.Secondary, icon = NixIcon.Pencil)
        }
    }
}

/** For a countable habit without a tracker yet. */
@Composable
internal fun MoneySavedInvite(onAdd: () -> Unit) {
    val s = LocalStrings.current
    val c = Nix.colors
    NixCard(onClick = onAdd) {
        Text(s.moneyTitle, style = NixType.BodyStrong, color = c.textPrimary)
        Text(s.moneyAddCta, style = NixType.Small, color = c.textSecondary)
    }
}

@Composable
private fun StreakBars(streaks: List<StreakSaving>, currency: String) {
    val s = LocalStrings.current
    val c = Nix.colors
    val max = streaks.maxOf { it.savedMinor }.coerceAtLeast(1)
    val summary = streaks.joinToString { Currencies.format(it.savedMinor, currency) }
    Canvas(
        Modifier.fillMaxWidth().height(96.dp).semantics { contentDescription = "${s.moneyPerStreak}: $summary" },
    ) {
        val gap = 8.dp.toPx()
        val w = (size.width - gap * (streaks.size - 1)) / streaks.size
        val radius = CornerRadius(6.dp.toPx())
        streaks.forEachIndexed { i, st ->
            val h = (size.height * st.savedMinor / max).coerceAtLeast(3.dp.toPx())
            val color = if (st.isCurrent) c.accentWarm else c.positive.copy(alpha = 0.55f)
            drawRoundRect(color, Offset(i * (w + gap), size.height - h), Size(w, h), radius)
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        Text(s.moneyNow, style = NixType.Smaller, color = c.accentWarm)
    }
}
