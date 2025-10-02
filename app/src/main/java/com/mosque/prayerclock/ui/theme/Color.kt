package com.mosque.prayerclock.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Mosque Clock Theme Colors
 * 
 * A comprehensive color system for the mosque prayer clock application.
 * This centralized color palette ensures consistency across the app and
 * makes it easy to create new themes or adjust the visual style.
 */

// ==================== Primary Theme Colors ====================

/**
 * Primary Accent Color - Used for premium accents, text, and decorative elements
 * Default: Brass/Gold (#B08D57)
 * Represents elegance and traditional mosque aesthetics
 */
val ColorPrimaryAccent = Color(0xFFB08D57)
val ColorPrimaryAccentLight = Color(0xFFC4A56E)
val ColorPrimaryAccentDark = Color(0xFF9C7A45)

/**
 * Surface Colors - Used for backgrounds, cards, and elevated surfaces
 * Default: Forest Green shades
 * Represents tranquility and Islamic tradition
 */
val ColorSurfacePrimary = Color(0xFF2D4A22)      // Deep surface
val ColorSurfaceSecondary = Color(0xFF3A5F2A)    // Medium surface
val ColorSurfaceTertiary = Color(0xFF4A7234)     // Light surface
val ColorSurfaceDark = Color(0xFF1A2E0A)         // Darkest surface
val ColorSurfaceCenter = Color(0xFF2D5016)       // Center/focal surface

/**
 * Secondary Accent Color - Used for secondary hands, subtle accents
 * Default: Cream/Beige (#F5F5DC)
 * Provides subtle contrast against dark backgrounds
 */
val ColorSecondaryAccent = Color(0xFFF5F5DC)

// ==================== Prayer Status Colors ====================

/**
 * Azan Time Indicator - Indicates the time for the call to prayer
 * Default: Bright green (#4CAF50)
 */
val ColorAzanTime = Color(0xFF4CAF50)

/**
 * Iqamah Time Indicator - Indicates when the congregation prayer begins
 * Default: Bright blue (#2196F3)
 */
val ColorIqamahTime = Color(0xFF2196F3)

/**
 * Sunrise Time Indicator - Special indicator for sunrise time (not a prayer)
 * Default: Orange/Red (#FF5722)
 */
val ColorSunriseTime = Color(0xFFFF5722)

// ==================== Background & Surface Colors ====================

/**
 * Pure Black - Used for main background to maximize contrast on TV displays
 */
val BackgroundBlack = Color(0xFF000000)

/**
 * Dark Gray - Used for secondary surfaces and cards
 */
val SurfaceDarkGray = Color(0xFF1A1A1A)

/**
 * Card Background - Used for elevated components
 */
val CardBackground = Color(0xFF2D2D2D)

// ==================== Standard UI Colors ====================

/**
 * Standard Green Colors - Used for success states and active elements
 */
val GreenBright = Color(0xFF66BB6A)
val GreenMedium = Color(0xFF81C784)
val GreenDark = Color(0xFF4CAF50)

/**
 * Pure White - Used for primary text and icons
 */
val TextWhite = Color(0xFFFFFFFF)

/**
 * Semi-transparent variations for overlays and shadows
 */
fun Color.withAlpha(alpha: Float): Color = this.copy(alpha = alpha)

// ==================== Shadow & Effect Colors ====================

/**
 * Shadow colors for depth and dimension
 */
val ShadowBlack = Color(0xFF000000)

/**
 * Overlay colors for gradients and effects
 */
val OverlayDark = Color(0xFF000000)

// ==================== Helper Extensions ====================

/**
 * Common alpha values for consistency
 */
object AlphaValues {
    const val SUBTLE = 0.3f
    const val MEDIUM = 0.6f
    const val STRONG = 0.85f
}

