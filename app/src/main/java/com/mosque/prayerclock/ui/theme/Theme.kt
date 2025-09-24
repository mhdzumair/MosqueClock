package com.mosque.prayerclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Clean dark color scheme for mosque displays
private val MosqueClockColorScheme =
    darkColorScheme(
        primary = Color(0xFF66BB6A), // Brighter green for better visibility
        secondary = Color(0xFF81C784),
        tertiary = Color(0xFF4CAF50),
        background = Color(0xFF000000), // Pure black for maximum contrast
        surface = Color(0xFF1A1A1A), // Slightly lighter than background
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White,
    )

@Composable
fun MosqueClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MosqueClockColorScheme,
        typography = Typography,
        content = content,
    )
}
