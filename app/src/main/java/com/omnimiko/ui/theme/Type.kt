package com.omnimiko.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/** Minimal type scale; relies on platform fonts to keep the APK small. */
val OmniTypography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, fontFamily = FontFamily.SansSerif),
    bodyLarge = TextStyle(fontSize = 16.sp, fontFamily = FontFamily.SansSerif),
    bodyMedium = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.SansSerif),
    labelSmall = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
)
