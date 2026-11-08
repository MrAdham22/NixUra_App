@file:OptIn(ExperimentalLayoutApi::class)

package com.adhamelgendy.nixura.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.domain.money.CostUnit
import com.adhamelgendy.nixura.domain.money.CountableProfile
import com.adhamelgendy.nixura.domain.money.Currencies
import com.adhamelgendy.nixura.presentation.components.NixCard
import com.adhamelgendy.nixura.presentation.components.NixChip
import com.adhamelgendy.nixura.presentation.components.NixSwitch
import com.adhamelgendy.nixura.presentation.components.NixTextField
import com.adhamelgendy.nixura.presentation.habits.CostInput
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType

/**
 * Optional "money saved" questions, shown only for habits that are bought and counted ([profile] from
 * CountableHabits). Off by default; everything here is optional and editable later.
 */
@Composable
internal fun HabitCostSection(profile: CountableProfile, input: CostInput, showErrors: Boolean, onChange: (CostInput) -> Unit) {
    val s = LocalStrings.current
    val c = Nix.colors
    val unitName = s.costUnitName(input.unit, plural = true)
    NixCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.moneyOptIn, style = NixType.BodyStrong, color = c.textPrimary)
                Text(s.moneyOptInBody, style = NixType.Small, color = c.textMuted)
            }
            Spacer(Modifier.width(12.dp))
            NixSwitch(input.enabled, { onChange(input.copy(enabled = it)) })
        }
        AnimatedVisibility(input.enabled) {
            Column(Modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Spacer(Modifier)
                NixTextField(
                    input.item, { onChange(input.copy(item = it)) },
                    label = s.moneyWhatExactly,
                    placeholder = if (input.unit == CostUnit.CIGARETTE) s.moneyWhatCigarettes else s.moneyWhatOther,
                    maxLength = 60,
                )
                if (profile.units.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(s.moneyBoughtAs, style = NixType.SmallStrong, color = c.textSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            profile.units.forEach { u ->
                                NixChip(s.costUnitName(u, plural = false).replaceFirstChar { it.uppercase() }, u == input.unit, { onChange(input.copy(unit = u)) })
                            }
                        }
                    }
                }
                if (profile.packs) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(s.moneyCountBy, style = NixType.SmallStrong, color = c.textSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NixChip(unitName.replaceFirstChar { it.uppercase() }, !input.inPacks, { onChange(input.copy(inPacks = false)) })
                            NixChip(s.moneyPacksPerDay, input.inPacks, { onChange(input.copy(inPacks = true)) })
                        }
                    }
                }
                NixTextField(
                    input.perDay, { onChange(input.copy(perDay = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' })) },
                    label = if (input.inPacks) s.moneyPacksPerDay else s.moneyPerDay,
                    placeholder = if (input.inPacks) "1" else "10",
                    keyboardType = KeyboardType.Decimal,
                    maxLength = 6,
                )
                if (profile.packs) {
                    NixTextField(
                        input.perPack, { onChange(input.copy(perPack = it.filter { ch -> ch.isDigit() })) },
                        label = s.moneyPerPack,
                        placeholder = profile.defaultPerPack.toString(),
                        keyboardType = KeyboardType.Number,
                        maxLength = 4,
                    )
                }
                NixTextField(
                    input.price, { onChange(input.copy(price = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' })) },
                    label = s.moneyPricePer(if (profile.packs) s.packWord else s.costUnitName(input.unit, plural = false)) +
                        " (${Currencies.of(input.currency).symbol})",
                    placeholder = "0",
                    keyboardType = KeyboardType.Decimal,
                    maxLength = 10,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(s.moneyCurrency, style = NixType.SmallStrong, color = c.textSecondary)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Currencies.all.forEach { cur ->
                            NixChip(cur.code, cur.code == input.currency, { onChange(input.copy(currency = cur.code)) })
                        }
                    }
                }
                if (showErrors && !input.isComplete) {
                    Text(s.moneyInvalid, style = NixType.Small, color = c.danger)
                }
            }
        }
    }
}
