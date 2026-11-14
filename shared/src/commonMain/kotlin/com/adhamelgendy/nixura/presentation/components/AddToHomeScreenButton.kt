package com.adhamelgendy.nixura.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.domain.widget.WidgetKind
import com.adhamelgendy.nixura.domain.widget.WidgetPinResult
import com.adhamelgendy.nixura.domain.widget.WidgetPinner
import com.adhamelgendy.nixura.presentation.strings.LocalStrings
import com.adhamelgendy.nixura.presentation.theme.Nix
import com.adhamelgendy.nixura.presentation.theme.NixType
import org.koin.mp.KoinPlatform

/**
 * "Add to Home screen": asks the launcher to place a widget for this habit/event. Shows nothing on platforms with
 * no [WidgetPinner] (iOS for now), so it is safe to drop into any detail screen.
 */
@Composable
fun AddToHomeScreenButton(kind: WidgetKind, targetId: Long?, modifier: Modifier = Modifier) {
    val pinner = remember { runCatching { KoinPlatform.getKoin().getOrNull<WidgetPinner>() }.getOrNull() } ?: return
    val text = LocalStrings.current
    var note by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NixButton(
            text.widgetAddToHome,
            {
                note = when (pinner.requestPin(kind, targetId)) {
                    WidgetPinResult.REQUESTED -> text.widgetAddToHomeAsk
                    WidgetPinResult.UNSUPPORTED -> text.widgetAddToHomeManual
                    WidgetPinResult.FAILED -> text.widgetAddToHomeFailed
                }
            },
            style = NixButtonStyle.Ghost,
            icon = NixIcon.Home,
            modifier = Modifier.fillMaxWidth(),
        )
        note?.let { Text(it, style = NixType.Smaller, color = Nix.colors.textMuted) }
    }
}

/**
 * "Add to Home screen" as an action for a card's sheet. Null on platforms with no [WidgetPinner] (iOS for now), so
 * callers simply leave the option out. When the launcher cannot pin, a short sheet says how to add it by hand.
 */
@Composable
fun rememberAddToHomeAction(): ((WidgetKind, Long) -> Unit)? {
    val pinner = remember { runCatching { KoinPlatform.getKoin().getOrNull<WidgetPinner>() }.getOrNull() } ?: return null
    val text = LocalStrings.current
    var note by remember { mutableStateOf<String?>(null) }
    note?.let { InfoSheet(text.widgetAddToHome, null, it, onDismiss = { note = null }) }
    return { kind, id ->
        note = when (pinner.requestPin(kind, id)) {
            WidgetPinResult.REQUESTED -> null // the launcher shows its own "Add to Home screen" dialog
            WidgetPinResult.UNSUPPORTED -> text.widgetAddToHomeManual
            WidgetPinResult.FAILED -> text.widgetAddToHomeFailed
        }
    }
}
