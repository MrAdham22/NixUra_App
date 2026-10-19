package com.example.reclaimapp.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NixUraDarkScheme = darkColorScheme(
    primary = NixUraPalette.Rose,
    onPrimary = NixUraPalette.onLightPrimary,
    primaryContainer = NixUraPalette.Plum,
    onPrimaryContainer = NixUraPalette.onDarkPrimary,
    secondary = NixUraPalette.Orange,
    onSecondary = NixUraPalette.onLightPrimary,
    secondaryContainer = NixUraPalette.Purple,
    onSecondaryContainer = NixUraPalette.onDarkPrimary,
    tertiary = NixUraPalette.Blush,
    onTertiary = NixUraPalette.onLightPrimary,
    background = NixUraPalette.Navy,
    onBackground = NixUraPalette.onDarkPrimary,
    surface = NixUraPalette.Purple,
    onSurface = NixUraPalette.onDarkPrimary,
    surfaceVariant = NixUraPalette.Plum.copy(alpha = 0.35f),
    onSurfaceVariant = NixUraPalette.onDarkMuted,
    outline = NixUraPalette.Rose.copy(alpha = 0.45f),
    outlineVariant = NixUraPalette.Purple.copy(alpha = 0.6f),
    error = NixUraPalette.Error,
    onError = NixUraPalette.Blush,
)

private val NixUraTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
    ),
)

private val NixUraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun NixUraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NixUraDarkScheme,
        typography = NixUraTypography,
        shapes = NixUraShapes,
        content = content,
    )
}
