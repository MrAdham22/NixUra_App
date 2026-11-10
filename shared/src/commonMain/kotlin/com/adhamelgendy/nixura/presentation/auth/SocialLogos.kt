package com.adhamelgendy.nixura.presentation.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adhamelgendy.nixura.domain.auth.SocialProvider

/**
 * The providers' own marks, drawn from their published 24×24 outlines in their own colours (brand rules: the marks
 * are not recoloured or redrawn). Decorative here: the button around them carries the label for screen readers.
 */
@Composable
fun SocialLogo(provider: SocialProvider, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val parts = remember(provider) {
        when (provider) {
            SocialProvider.Google -> GOOGLE
            SocialProvider.Facebook -> FACEBOOK
        }.map { (color, data) -> color to PathParser().parsePathString(data).toPath() }
    }
    Canvas(modifier.size(size)) {
        scale(this.size.width / 24f, this.size.height / 24f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            // Facebook's "f" is a hole in its blue disc; a white disc behind shows it white on any background.
            if (provider == SocialProvider.Facebook) drawCircle(Color.White, radius = 11f, center = androidx.compose.ui.geometry.Offset(12f, 12f))
            parts.forEach { (color, path: Path) -> drawPath(path, color) }
        }
    }
}

private val GOOGLE = listOf(
    Color(0xFF4285F4) to "M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z",
    Color(0xFF34A853) to "M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z",
    Color(0xFFFBBC05) to "M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z",
    Color(0xFFEA4335) to "M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z",
)

private val FACEBOOK = listOf(
    Color(0xFF1877F2) to "M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z",
)
