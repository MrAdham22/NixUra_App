package com.adhamelgendy.nixura.presentation.goodhabits.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.domain.theme.WaterUnit
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixChip
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.NixIconButton
import com.adhamelgendy.nixura.presentation.strings.ArabicStrings
import com.adhamelgendy.nixura.presentation.strings.EnglishStrings
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.strings.Strings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import com.adhamelgendy.nixura.presentation.theme.NixUraTheme

/**
 * The daily water goal on the Add/Edit Good Habit form: a Liters/Cups toggle, the goal stepper and the cup size.
 * Shown instead of the "daily target" stepper for a daily water habit. [cupSizeChanged] (editing only) adds a note that
 * past days will be counted with the new cup size.
 */
@Composable
fun WaterGoalSection(
    form: WaterGoalForm,
    onChange: (WaterGoalForm) -> Unit,
    modifier: Modifier = Modifier,
    cupSizeChanged: Boolean = false,
) {
    val s = LocalStrings.current
    val c = Nix.colors
    NixCard(modifier) {
        Text(s.waterGoalTitle, style = NixType.BodyStrong, color = c.textPrimary)
        Spacer(Modifier.height(2.dp))
        Text(s.waterGoalHint, style = NixType.Small, color = c.textMuted)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NixChip(s.waterUnitLiters, selected = form.unit == WaterUnit.LITERS, onClick = { onChange(form.withUnit(WaterUnit.LITERS)) })
            NixChip(s.waterUnitCups, selected = form.unit == WaterUnit.CUPS, onClick = { onChange(form.withUnit(WaterUnit.CUPS)) })
        }
        Spacer(Modifier.height(14.dp))
        val goalText = when (form.unit) {
            WaterUnit.LITERS -> s.waterLiters(form.litersText())
            WaterUnit.CUPS -> s.waterCups(form.cupsText(), form.servingsPerDay)
        }
        ValueStepper(
            label = s.waterGoalTitle,
            value = goalText,
            onDecrease = { onChange(form.stepGoal(up = false)) },
            onIncrease = { onChange(form.stepGoal(up = true)) },
            canDecrease = form.canDecreaseGoal,
            canIncrease = form.canIncreaseGoal,
            decreaseLabel = s.waterGoalDecrease,
            increaseLabel = s.waterGoalIncrease,
            hideLabel = true,
        )
        Spacer(Modifier.height(14.dp))
        ValueStepper(
            label = s.waterCupSize,
            hint = s.waterCupSizeHint,
            value = s.waterMl(form.servingMl),
            onDecrease = { onChange(form.stepServing(up = false)) },
            onIncrease = { onChange(form.stepServing(up = true)) },
            canDecrease = form.canDecreaseServing,
            canIncrease = form.canIncreaseServing,
            decreaseLabel = s.waterCupSize,
            increaseLabel = s.waterCupSize,
        )
        Spacer(Modifier.height(12.dp))
        Text(s.waterGoalSummary(form.servingsPerDay, form.servingMl), style = NixType.SmallStrong, color = c.water)
        if (cupSizeChanged) {
            Spacer(Modifier.height(6.dp))
            Text(s.waterCupSizeChanged, style = NixType.Small, color = c.accentWarm)
        }
    }
}

@Composable
private fun ValueStepper(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    canDecrease: Boolean,
    canIncrease: Boolean,
    decreaseLabel: String,
    increaseLabel: String,
    hint: String? = null,
    hideLabel: Boolean = false,
) {
    val c = Nix.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (!hideLabel) Text(label, style = NixType.BodyStrong, color = c.textPrimary)
            if (hint != null) {
                Spacer(Modifier.height(2.dp))
                Text(hint, style = NixType.Small, color = c.textMuted)
            }
        }
        NixIconButton(
            NixIcon.Minus,
            { if (canDecrease) onDecrease() },
            modifier = Modifier.semantics { contentDescription = decreaseLabel },
            tint = if (canDecrease) c.textPrimary else c.textMuted.copy(alpha = 0.5f),
            size = 40.dp,
        )
        Text(value, style = NixType.Heading, color = c.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.width(104.dp), maxLines = 1)
        NixIconButton(
            NixIcon.Plus,
            { if (canIncrease) onIncrease() },
            modifier = Modifier.semantics { contentDescription = increaseLabel },
            tint = if (canIncrease) c.textPrimary else c.textMuted.copy(alpha = 0.5f),
            size = 40.dp,
        )
    }
}

// ---- Previews: dark + light, LTR + RTL ----

@Composable
private fun WaterPreviewFrame(dark: Boolean, rtl: Boolean, content: @Composable ColumnScope.() -> Unit) {
    NixUraTheme(darkTheme = dark) {
        val strings: Strings = if (rtl) ArabicStrings else EnglishStrings
        CompositionLocalProvider(
            LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            LocalStrings provides strings,
        ) {
            Column(Modifier.background(Nix.colors.background).padding(20.dp), content = content)
        }
    }
}

@Composable
private fun WaterSectionSample(unit: WaterUnit, changed: Boolean = false) {
    var form by remember { mutableStateOf(WaterGoalForm(unit = unit)) }
    WaterGoalSection(form, { form = it }, cupSizeChanged = changed)
}

@Preview
@Composable
private fun WaterGoalSectionDarkPreview() = WaterPreviewFrame(dark = true, rtl = false) { WaterSectionSample(WaterUnit.CUPS) }

@Preview
@Composable
private fun WaterGoalSectionLightPreview() = WaterPreviewFrame(dark = false, rtl = false) { WaterSectionSample(WaterUnit.LITERS, changed = true) }

@Preview
@Composable
private fun WaterGoalSectionDarkRtlPreview() = WaterPreviewFrame(dark = true, rtl = true) { WaterSectionSample(WaterUnit.LITERS) }

@Preview
@Composable
private fun WaterGoalSectionLightRtlPreview() = WaterPreviewFrame(dark = false, rtl = true) { WaterSectionSample(WaterUnit.CUPS, changed = true) }
