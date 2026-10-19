package com.example.reclaimapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.reclaimapp.presentation.screen.DesignPreviewScreen
import com.example.reclaimapp.presentation.theme.NixUraTheme

@Composable
@Preview
fun App() {
    NixUraTheme {
        DesignPreviewScreen(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize(),
        )
    }
}
