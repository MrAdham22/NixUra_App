package com.adhamelgendy.nixura.presentation.goodhabits.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.presentation.appearance.HabitThemes
import com.adhamelgendy.nixura.presentation.components.NixTextField
import com.adhamelgendy.nixura.presentation.components.themeanim.HabitThemeBackdrop
import com.adhamelgendy.nixura.presentation.strings.ArabicStrings
import com.adhamelgendy.nixura.presentation.strings.EnglishStrings
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixUraTheme

/** The still frame of each theme background behind a name field, as "remove animations" shows it. */
@Composable
private fun BackdropPreview(theme: HabitTheme, name: String, dark: Boolean, rtl: Boolean) {
    NixUraTheme(darkTheme = dark) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            LocalStrings provides if (rtl) ArabicStrings else EnglishStrings,
        ) {
            val colors = Nix.colors
            val visual = HabitThemes.visualFor(theme, colors)
            HabitThemeBackdrop(
                kind = visual.animation,
                accent = visual.accent,
                colors = colors,
                modifier = Modifier.fillMaxWidth().height(520.dp).background(colors.background),
                still = true,
            ) {
                Column(Modifier.padding(20.dp)) {
                    NixTextField(value = name, onValueChange = {}, label = LocalStrings.current.goodHabitNameLabel)
                    if (theme == HabitTheme.WATER) WaterGoalSection(WaterGoalForm(), {}, Modifier.padding(top = 18.dp))
                }
            }
        }
    }
}

@Preview @Composable private fun WaterBackdropDark() = BackdropPreview(HabitTheme.WATER, "Drink water", dark = true, rtl = false)
@Preview @Composable private fun WaterBackdropLight() = BackdropPreview(HabitTheme.WATER, "Drink water", dark = false, rtl = false)
@Preview @Composable private fun WaterBackdropDarkRtl() = BackdropPreview(HabitTheme.WATER, "اشرب مياه", dark = true, rtl = true)
@Preview @Composable private fun WaterBackdropLightRtl() = BackdropPreview(HabitTheme.WATER, "اشرب مياه", dark = false, rtl = true)
@Preview @Composable private fun WalkBackdropDark() = BackdropPreview(HabitTheme.WALK, "Walk 30 minutes", dark = true, rtl = false)
@Preview @Composable private fun WalkBackdropLightRtl() = BackdropPreview(HabitTheme.WALK, "المشي", dark = false, rtl = true)
@Preview @Composable private fun WorkoutBackdropDark() = BackdropPreview(HabitTheme.WORKOUT, "Workout", dark = true, rtl = false)
@Preview @Composable private fun ReadingBackdropLight() = BackdropPreview(HabitTheme.READING, "Read 10 pages", dark = false, rtl = false)
@Preview @Composable private fun SleepBackdropDark() = BackdropPreview(HabitTheme.SLEEP, "Sleep early", dark = true, rtl = false)
@Preview @Composable private fun SleepBackdropLightRtl() = BackdropPreview(HabitTheme.SLEEP, "النوم مبكرًا", dark = false, rtl = true)
@Preview @Composable private fun PrayerRaysDark() = BackdropPreview(HabitTheme.PRAYER, "Pray", dark = true, rtl = false)
