package com.upipulse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.upipulse.domain.model.AppTheme

private val DarkColors = darkColorScheme(
    primary = Color(0xFF818CF8), // Indigo 400
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF312E81), // Indigo 900
    onPrimaryContainer = Color(0xFFE0E7FF), // Indigo 100
    secondary = Color(0xFF38BDF8), // Sky 400
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0C4A6E), // Sky 900
    onSecondaryContainer = Color(0xFFE0F2FE), // Sky 100
    tertiary = Color(0xFFF472B6), // Pink 400
    onTertiary = Color.Black,
    surface = Color(0xFF0F172A), // Slate 900
    onSurface = Color(0xFFF1F5F9), // Slate 100
    background = Color(0xFF020617) // Slate 950
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5), // Indigo 600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF), // Indigo 50 side
    onPrimaryContainer = Color(0xFF3730A3), // Indigo 800
    secondary = Color(0xFF0EA5E9), // Sky 500
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE), // Sky 100
    onSecondaryContainer = Color(0xFF075985), // Sky 800
    tertiary = Color(0xFFDB2777), // Pink 600
    onTertiary = Color.White,
    surface = Color.White,
    onSurface = Color(0xFF1E293B), // Slate 800
    background = Color(0xFFF8FAFC) // Slate 50
)

@Composable
fun TrackItTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    
    val colors = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
