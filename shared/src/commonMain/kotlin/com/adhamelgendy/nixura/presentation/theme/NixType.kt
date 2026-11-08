package com.adhamelgendy.nixura.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Text styles used by the NixUra UI. Colors are applied at the call site. */
object NixType {
    val Display = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
    val Title = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
    val Heading = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
    val Body = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.1).sp)
    val BodyStrong = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp)
    val Small = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)

    val Smaller = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Normal)
    val SmallStrong = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
    val Eyebrow = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.0.sp)
    val Button = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    fun counter(size: Int) = TextStyle(
        fontSize = size.sp,
        lineHeight = (size * 1.1f).sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
    )
}
