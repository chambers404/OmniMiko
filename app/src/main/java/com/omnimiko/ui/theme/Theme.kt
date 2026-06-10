package com.omnimiko.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Violet = Color(0xFF7C5CFF)
private val VioletDark = Color(0xFF5A3FE0)
private val Ink = Color(0xFF0E1116)
private val Surface = Color(0xFF161A22)

private val DarkColors = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    secondary = Color(0xFF35E0C0),
    background = Ink,
    surface = Surface,
    onBackground = Color(0xFFE6E8EC),
    onSurface = Color(0xFFE6E8EC),
)

private val LightColors = lightColorScheme(
    primary = VioletDark,
    onPrimary = Color.White,
    secondary = Color(0xFF0E9E86),
    background = Color(0xFFF7F7FA),
    surface = Color.White,
)

@Composable
fun OmniMikoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = OmniTypography,
        content = content,
    )
}
