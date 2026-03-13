package com.upipulse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7C3AED),
    secondary = Color(0xFF38BDF8),
    tertiary = Color(0xFFF97316)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4C1D95),
    secondary = Color(0xFF0EA5E9),
    tertiary = Color(0xFFEA580C)
)

@Composable
fun UpiPulseTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
