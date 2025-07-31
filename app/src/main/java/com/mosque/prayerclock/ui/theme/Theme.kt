package com.mosque.prayerclock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = Color(0xFF81C784),
    tertiary = Color(0xFF2E7D32),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    secondary = Color(0xFF4CAF50),
    tertiary = Color(0xFF81C784),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

private val MosqueGreenColorScheme = darkColorScheme(
    primary = Color(0xFF1B5E20),
    secondary = Color(0xFF2E7D32),
    tertiary = Color(0xFF4CAF50),
    background = Color(0xFF0D2E18),
    surface = Color(0xFF1B4332),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val BlueColorScheme = darkColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF42A5F5),
    tertiary = Color(0xFF90CAF9),
    background = Color(0xFF0D1B2E),
    surface = Color(0xFF1A3952),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun MosqueClockTheme(
    theme: com.mosque.prayerclock.data.model.AppTheme = com.mosque.prayerclock.data.model.AppTheme.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        com.mosque.prayerclock.data.model.AppTheme.LIGHT -> LightColorScheme
        com.mosque.prayerclock.data.model.AppTheme.DARK -> DarkColorScheme
        com.mosque.prayerclock.data.model.AppTheme.MOSQUE_GREEN -> MosqueGreenColorScheme
        com.mosque.prayerclock.data.model.AppTheme.BLUE -> BlueColorScheme
        com.mosque.prayerclock.data.model.AppTheme.DEFAULT -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}