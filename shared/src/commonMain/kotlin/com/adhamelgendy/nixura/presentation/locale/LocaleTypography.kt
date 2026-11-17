package com.adhamelgendy.nixura.presentation.locale

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import nixura.shared.generated.resources.Res
import nixura.shared.generated.resources.ibm_plex_sans_arabic_bold
import nixura.shared.generated.resources.ibm_plex_sans_arabic_regular
import nixura.shared.generated.resources.ibm_plex_sans_arabic_semibold
import org.jetbrains.compose.resources.Font

/** IBM Plex Sans Arabic (SIL Open Font License, bundled in composeResources/font), shared with iOS. */
@Composable
fun rememberArabicFontFamily(): FontFamily = FontFamily(
    Font(Res.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(Res.font.ibm_plex_sans_arabic_semibold, FontWeight.Medium),
    Font(Res.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
    Font(Res.font.ibm_plex_sans_arabic_bold, FontWeight.Bold),
)

/** Arabic letters (not the Arabic-Indic digits, which are as short as Latin ones). */
fun hasArabicLetters(text: CharSequence): Boolean =
    text.any { it in '\u0600'..'\u06FF' && it !in '\u0660'..'\u066C' || it in '\u0750'..'\u077F' || it in '\uFB50'..'\uFDFF' || it in '\uFE70'..'\uFEFF' }

/** Arabic line height: at least 1.5 × the font size, so marks above and below the letters are never clipped. */
internal const val ARABIC_LINE_HEIGHT = 1.5f

/**
 * [style] adjusted for the language: in Arabic (or for any text with Arabic letters) the Arabic font, no letter
 * spacing (tracking breaks the joined letters), and a taller line for text that has Arabic letters. English text keeps
 * the system font and the style exactly as it is.
 */
fun AppLocale.style(style: TextStyle, text: CharSequence? = null): TextStyle {
    val arabicText = text != null && hasArabicLetters(text)
    if (!isArabic && !arabicText) return style
    val family = arabicFont ?: style.fontFamily
    val tall = arabicText || text == null
    val size = style.fontSize
    val lineHeight = if (tall && size.isSpecified && size.isSp) {
        val min = (size.value * ARABIC_LINE_HEIGHT).sp
        val current = style.lineHeight
        if (!current.isSpecified || (current.isSp && current.value < min.value)) min else current
    } else {
        style.lineHeight
    }
    return style.copy(
        fontFamily = family,
        letterSpacing = 0.sp,
        lineHeight = lineHeight,
        lineHeightStyle = if (tall) LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None) else style.lineHeightStyle,
    )
}

/** A caller's explicit letter spacing, dropped where it would pull Arabic letters apart. */
fun AppLocale.letterSpacing(value: TextUnit, text: CharSequence?): TextUnit =
    if (isArabic || (text != null && hasArabicLetters(text))) TextUnit.Unspecified else value
