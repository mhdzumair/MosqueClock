package com.mosque.prayerclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun MosqueClockTheme(
    colorTheme: ColorTheme = AppColorThemes.ClassicMosque,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalColorTheme provides colorTheme) {
        // Create dynamic color scheme based on selected theme
        val dynamicColorScheme =
            darkColorScheme(
                primary = colorTheme.uiPrimary, // Primary UI color from theme
                secondary = colorTheme.uiSecondary, // Secondary UI from theme
                tertiary = colorTheme.uiTertiary, // Tertiary UI from theme
                background = colorTheme.backgroundMain, // Pure black for maximum contrast
                surface = colorTheme.surfaceCard, // Card surface from theme
                onPrimary = colorTheme.textPrimary, // Text on primary
                onSecondary = colorTheme.textPrimary, // Text on secondary
                onTertiary = colorTheme.textPrimary, // Text on tertiary
                onBackground = colorTheme.textPrimary, // Text on background
                onSurface = colorTheme.textPrimary, // Text on surface
                primaryContainer = colorTheme.uiPrimary.copy(alpha = 0.3f), // Container colors
                secondaryContainer = colorTheme.uiSecondary.copy(alpha = 0.3f),
                tertiaryContainer = colorTheme.uiTertiary.copy(alpha = 0.3f),
            )

        MaterialTheme(
            colorScheme = dynamicColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
