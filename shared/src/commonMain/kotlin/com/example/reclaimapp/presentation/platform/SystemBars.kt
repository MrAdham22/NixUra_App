package com.example.reclaimapp.presentation.platform

import androidx.compose.runtime.Composable

/** Makes status/navigation bar icons readable for the in-app theme (independent of the system theme). */
@Composable
expect fun SystemBarsEffect(darkTheme: Boolean)
