package com.mosque.prayerclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Clean dark color scheme for mosque displays
private val MosqueClockColorScheme =
    darkColorScheme(
        primary = GreenBright, // Brighter green for better visibility
        secondary = GreenMedium,
        tertiary = GreenDark,
        background = BackgroundBlack, // Pure black for maximum contrast
        surface = SurfaceDarkGray, // Slightly lighter than background
        onPrimary = TextWhite,
        onSecondary = TextWhite,
        onTertiary = TextWhite,
        onBackground = TextWhite,
        onSurface = TextWhite,
    )

@Composable
fun MosqueClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MosqueClockColorScheme,
        typography = Typography,
        content = content,
    )
}
