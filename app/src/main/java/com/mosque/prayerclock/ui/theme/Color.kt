package com.mosque.prayerclock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Mosque Clock Theme Colors
 *
 * Dynamic color system that adapts based on the selected theme.
 * These are composable properties that read from the current theme.
 *
 * Access colors from the currently active theme
 */
@Composable
@ReadOnlyComposable
private fun currentTheme(): ColorTheme = LocalColorTheme.current

// ==================== Dynamic Theme Colors ====================

/**
 * Primary Accent Color - Used for premium accents, text, and decorative elements
 */
val ColorPrimaryAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().primaryAccent

val ColorPrimaryAccentLight: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().primaryAccentLight

val ColorPrimaryAccentDark: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().primaryAccentDark

/**
 * Surface Colors - Used for backgrounds, cards, and elevated surfaces
 */
val ColorSurfacePrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().surfacePrimary

val ColorSurfaceSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().surfaceSecondary

val ColorSurfaceTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().surfaceTertiary

val ColorSurfaceDark: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().surfaceDark

val ColorSurfaceCenter: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().surfaceCenter

/**
 * Secondary Accent Color - Used for secondary hands, subtle accents
 */
val ColorSecondaryAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().secondaryAccent

// ==================== Prayer Status Colors ====================

/**
 * Azan Time Indicator - Indicates the time for the call to prayer
 */
val ColorAzanTime: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().azanTime

/**
 * Iqamah Time Indicator - Indicates when the congregation prayer begins
 */
val ColorIqamahTime: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().iqamahTime

/**
 * Sunrise Time Indicator - Special indicator for sunrise time (not a prayer)
 */
val ColorSunriseTime: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().sunriseTime

/**
 * Next Prayer Azan Time - Brighter version for highlighting the next azan
 */
val ColorNextAzanTime: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().nextAzanTime

/**
 * Next Prayer Iqamah Time - Brighter version for highlighting the next iqamah
 */
val ColorNextIqamahTime: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().nextIqamahTime

/**
 * Next Prayer Sunrise Time - Brighter version for highlighting next sunrise
 */
val ColorNextSunriseTime: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().nextSunriseTime

// ==================== Background & Surface Colors (Theme-based) ====================

val ColorBackgroundMain: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().backgroundMain

val ColorSurfaceCard: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().surfaceCard

val ColorCardBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().cardBackground

// ==================== UI Colors (Theme-based) ====================

val ColorUIPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().uiPrimary

val ColorUISecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().uiSecondary

val ColorUITertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().uiTertiary

// ==================== Text Colors (Theme-based) ====================

val ColorTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().textPrimary

val ColorTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().textSecondary

// ==================== Shadow & Effect Colors (Theme-based) ====================

val ColorShadow: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().shadowColor

val ColorOverlay: Color
    @Composable
    @ReadOnlyComposable
    get() = currentTheme().overlayColor

/**
 * Data class to hold ALL theme colors for use in non-@Composable contexts like Canvas
 */
data class ThemeColors(
    val primaryAccent: Color,
    val primaryAccentLight: Color,
    val primaryAccentDark: Color,
    val surfacePrimary: Color,
    val surfaceSecondary: Color,
    val surfaceTertiary: Color,
    val surfaceDark: Color,
    val surfaceCenter: Color,
    val secondaryAccent: Color,
    val azanTime: Color,
    val iqamahTime: Color,
    val sunriseTime: Color,
    val nextAzanTime: Color,
    val nextIqamahTime: Color,
    val nextSunriseTime: Color,
    val backgroundMain: Color,
    val surfaceCard: Color,
    val cardBackground: Color,
    val uiPrimary: Color,
    val uiSecondary: Color,
    val uiTertiary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val shadowColor: Color,
    val overlayColor: Color,
)

/**
 * Helper function to get all theme colors at once (for use in non-@Composable contexts like Canvas)
 */
@Composable
fun rememberThemeColors() =
    ThemeColors(
        primaryAccent = ColorPrimaryAccent,
        primaryAccentLight = ColorPrimaryAccentLight,
        primaryAccentDark = ColorPrimaryAccentDark,
        surfacePrimary = ColorSurfacePrimary,
        surfaceSecondary = ColorSurfaceSecondary,
        surfaceTertiary = ColorSurfaceTertiary,
        surfaceDark = ColorSurfaceDark,
        surfaceCenter = ColorSurfaceCenter,
        secondaryAccent = ColorSecondaryAccent,
        azanTime = ColorAzanTime,
        iqamahTime = ColorIqamahTime,
        sunriseTime = ColorSunriseTime,
        nextAzanTime = ColorNextAzanTime,
        nextIqamahTime = ColorNextIqamahTime,
        nextSunriseTime = ColorNextSunriseTime,
        backgroundMain = ColorBackgroundMain,
        surfaceCard = ColorSurfaceCard,
        cardBackground = ColorCardBackground,
        uiPrimary = ColorUIPrimary,
        uiSecondary = ColorUISecondary,
        uiTertiary = ColorUITertiary,
        textPrimary = ColorTextPrimary,
        textSecondary = ColorTextSecondary,
        shadowColor = ColorShadow,
        overlayColor = ColorOverlay,
    )

// ==================== Helper Extensions ====================

/**
 * Semi-transparent variations for overlays and shadows
 */
fun Color.withAlpha(alpha: Float): Color = this.copy(alpha = alpha)

/**
 * Common alpha values for consistency across all themes
 */
object AlphaValues {
    const val SUBTLE = 0.3f
    const val MEDIUM = 0.6f
    const val STRONG = 0.85f
    const val VERY_SUBTLE = 0.1f
}
