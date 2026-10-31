package com.example.reclaimapp.presentation.components

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
import com.example.reclaimapp.domain.widget.WidgetKind
import com.example.reclaimapp.domain.widget.WidgetPinResult
import com.example.reclaimapp.domain.widget.WidgetPinner
import com.example.reclaimapp.domain.widget.WidgetStrings
import com.example.reclaimapp.presentation.theme.Nix
import com.example.reclaimapp.presentation.theme.NixType
import org.koin.mp.KoinPlatform

/**
 * "Add to Home screen": asks the launcher to place a widget for this habit/event. Shows nothing on platforms with
 * no [WidgetPinner] (iOS for now), so it is safe to drop into any detail screen.
 */
@Composable
fun AddToHomeScreenButton(kind: WidgetKind, targetId: Long?, modifier: Modifier = Modifier) {
    val pinner = remember { runCatching { KoinPlatform.getKoin().getOrNull<WidgetPinner>() }.getOrNull() } ?: return
    val text = remember { WidgetStrings() }
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
            style = NixButtonStyle.Secondary,
            icon = NixIcon.Home,
            modifier = Modifier.fillMaxWidth(),
        )
        note?.let { Text(it, style = NixType.Smaller, color = Nix.colors.textMuted) }
    }
}
