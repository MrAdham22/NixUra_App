package com.example.reclaimapp.presentation.platform

import androidx.compose.runtime.Composable

/** True when the user asked the system to remove/reduce animations (Android "Remove animations", iOS "Reduce Motion"). */
@Composable
expect fun rememberReducedMotion(): Boolean
