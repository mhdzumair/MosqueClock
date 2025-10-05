package com.mosque.prayerclock.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * CompositionLocal for providing the current color theme throughout the app
 */
val LocalColorTheme = staticCompositionLocalOf { AppColorThemes.ClassicMosque }

/**
 * Color Theme System for Mosque Clock
 *
 * Each theme provides a complete color palette for the application,
 * ensuring visual consistency and allowing users to customize their experience.
 */

/**
 * Represents a complete color theme for the application
 * Optimized for TV viewing from long distances with high contrast
 */
data class ColorTheme(
    val id: String,
    val name: String,
    val description: String,
    // Primary accent colors - Used for premium accents, text, and decorative elements
    val primaryAccent: Color,
    val primaryAccentLight: Color,
    val primaryAccentDark: Color,
    // Surface colors for cards and backgrounds (clock face, flip digits, etc.)
    val surfacePrimary: Color,
    val surfaceSecondary: Color,
    val surfaceTertiary: Color,
    val surfaceDark: Color,
    val surfaceCenter: Color,
    // Secondary accent - Used for secondary hands, subtle accents
    val secondaryAccent: Color,
    // Prayer status indicators (must be highly visible from distance)
    val azanTime: Color,
    val iqamahTime: Color,
    val sunriseTime: Color,
    // Next prayer highlighting colors (brighter/more vibrant for emphasis)
    val nextAzanTime: Color,
    val nextIqamahTime: Color,
    val nextSunriseTime: Color,
    // Background and surface - TV optimized
    val backgroundMain: Color, // Main app background
    val surfaceCard: Color, // Card backgrounds
    val cardBackground: Color, // Elevated components
    // Standard UI Colors - Success states and active elements
    val uiPrimary: Color, // Primary UI color (buttons, switches)
    val uiSecondary: Color, // Secondary UI elements
    val uiTertiary: Color, // Tertiary UI elements
    // Text colors - High contrast for TV
    val textPrimary: Color, // Main text
    val textSecondary: Color, // Secondary text
    // Shadow and effect colors
    val shadowColor: Color, // Shadows for depth
    val overlayColor: Color, // Overlays and gradients
)

/**
 * Available color themes
 */
object AppColorThemes {
    /**
     * Classic Mosque Theme (Default)
     * Deep forest green with brass/gold accents
     * Traditional and elegant, perfect for mosque environments
     * Optimized for TV viewing with high contrast
     */
    val ClassicMosque =
        ColorTheme(
            id = "classic_mosque",
            name = "Classic Mosque",
            description = "Traditional forest green with elegant brass accents",
            // Brass/Gold accents - warm and elegant (original colors)
            primaryAccent = Color(0xFFB08D57), // Original brass/gold
            primaryAccentLight = Color(0xFFC4A56E), // Light brass
            primaryAccentDark = Color(0xFF9C7A45), // Dark brass
            // Deep forest green surfaces
            surfacePrimary = Color(0xFF2D4A22), // Deep forest green
            surfaceSecondary = Color(0xFF3A5F2A), // Medium forest green
            surfaceTertiary = Color(0xFF4A7234), // Light forest green
            surfaceDark = Color(0xFF1A2E0A), // Darkest green
            surfaceCenter = Color(0xFF2D5016), // Center green
            secondaryAccent = Color(0xFFF5F5DC), // Cream/Beige
            // Prayer status indicators (original colors)
            azanTime = Color(0xFF4CAF50), // Green (original)
            iqamahTime = Color(0xFF2196F3), // Blue (original)
            sunriseTime = Color(0xFFFF5722), // Orange/Red (original)
            // Next prayer highlighting colors (brighter versions for emphasis)
            nextAzanTime = Color(0xFF66BB6A), // Brighter green (Material Green 400)
            nextIqamahTime = Color(0xFF42A5F5), // Brighter blue (Material Blue 400)
            nextSunriseTime = Color(0xFFFF7043), // Brighter orange (Material Deep Orange 400)
            // Pure black background for maximum contrast
            backgroundMain = Color(0xFF000000), // Pure black
            surfaceCard = Color(0xFF1A1A1A), // Dark gray
            cardBackground = Color(0xFF2A2A2A), // Elevated dark gray
            // UI colors match original theme
            uiPrimary = Color(0xFF66BB6A), // Bright green (original Material Green 400)
            uiSecondary = Color(0xFF81C784), // Medium green (original Material Green 300)
            uiTertiary = Color(0xFF4CAF50), // Dark green (original Material Green 500)
            // High-contrast text
            textPrimary = Color(0xFFFFFFFF), // Pure white
            textSecondary = Color(0xFFB0B0B0), // Light gray (original)
            // Shadows and effects
            shadowColor = Color(0xFF000000), // Pure black shadow
            overlayColor = Color(0xFF000000), // Black overlay
        )

    /**
     * Midnight Blue Theme
     * Deep navy blue with bright silver/cyan accents
     * Modern and serene, inspired by night sky
     * Cool and calming for evening prayers, optimized for readability
     */
    val MidnightBlue =
        ColorTheme(
            id = "midnight_blue",
            name = "Midnight Blue",
            description = "Deep navy blue with elegant silver accents",
            // Bright silver accents for excellent TV visibility
            primaryAccent = Color(0xFFF0F0F0), // Very bright silver (almost white)
            primaryAccentLight = Color(0xFFFFFFFF), // Pure white
            primaryAccentDark = Color(0xFFD0D0D0), // Light silver
            // Deep navy surfaces with better contrast
            surfacePrimary = Color(0xFF1B3A5F), // Brighter navy (more visible)
            surfaceSecondary = Color(0xFF2A4A73), // Medium navy (lighter)
            surfaceTertiary = Color(0xFF3D5E8C), // Light navy (much lighter)
            surfaceDark = Color(0xFF0D1F35), // Darkest navy
            surfaceCenter = Color(0xFF1E3B5A), // Center navy
            secondaryAccent = Color(0xFFFFFFFF), // Pure white
            // Prayer indicators matching navy/blue theme (no green!)
            azanTime = Color(0xFF40C8FF), // Bright sky blue (matches theme)
            iqamahTime = Color(0xFF8FC7EA), // Very bright cyan-blue
            sunriseTime = Color(0xFFFFAA33), // Bright orange (sunrise always warm)
            // Next prayer highlighting colors (even brighter for emphasis)
            nextAzanTime = Color(0xFF5DD5FF), // Very bright sky blue
            nextIqamahTime = Color(0xFFB0E0FF), // Extremely bright cyan-blue
            nextSunriseTime = Color(0xFFFFCC66), // Very bright orange
            backgroundMain = Color(0xFF000000), // Pure black
            surfaceCard = Color(0xFF0D1A2D), // Dark navy (not gray)
            cardBackground = Color(0xFF162844), // Elevated navy
            // Bright UI elements for high contrast (blues match prayer indicators)
            uiPrimary = Color(0xFF5A9FD4), // Bright sky blue
            uiSecondary = Color(0xFF8FC7EA), // Very light blue
            uiTertiary = Color(0xFF3E7FA8), // Medium blue
            // Extra bright text for maximum contrast on dark navy
            textPrimary = Color(0xFFFFFFFF), // Pure white
            textSecondary = Color(0xFFE0E8F0), // Very bright blue-tinted white
            shadowColor = Color(0xFF000000),
            overlayColor = Color(0xFF000000),
        )

    /**
     * Desert Sand Theme
     * Warm beige and sand tones with copper accents
     * Inspired by Middle Eastern desert landscapes
     * Warm and inviting, excellent for daytime viewing
     */
    val DesertSand =
        ColorTheme(
            id = "desert_sand",
            name = "Desert Sand",
            description = "Warm desert tones with copper accents",
            // Bright copper/bronze accents
            primaryAccent = Color(0xFFE39A5D), // Bright copper (higher visibility)
            primaryAccentLight = Color(0xFFF5B678), // Light copper
            primaryAccentDark = Color(0xFFCD7F32), // Bronze
            // Warm brown surfaces
            surfacePrimary = Color(0xFF3E3228), // Deep brown
            surfaceSecondary = Color(0xFF4E3E2F), // Medium brown
            surfaceTertiary = Color(0xFF665542), // Light brown
            surfaceDark = Color(0xFF2A1F18), // Darkest brown
            surfaceCenter = Color(0xFF3A2D22), // Center brown
            secondaryAccent = Color(0xFFFFE4B5), // Moccasin (brighter beige)
            // Warm copper/bronze prayer indicators (match desert theme)
            azanTime = Color(0xFFFFA558), // Bright copper/orange (warm, no green)
            iqamahTime = Color(0xFFFFCC80), // Light copper/peach
            sunriseTime = Color(0xFFFF8A50), // Bright coral/orange
            // Next prayer highlighting colors (even warmer and brighter)
            nextAzanTime = Color(0xFFFFBF7A), // Very bright copper
            nextIqamahTime = Color(0xFFFFE0A0), // Very light copper/peach
            nextSunriseTime = Color(0xFFFFAA77), // Very bright coral
            backgroundMain = Color(0xFF000000), // Pure black
            surfaceCard = Color(0xFF1C1410), // Dark warm gray
            cardBackground = Color(0xFF2A2218), // Elevated warm gray
            // Warm copper/tan UI elements (match desert theme)
            uiPrimary = Color(0xFFCD7F32), // Bronze/copper
            uiSecondary = Color(0xFFE39A5D), // Light copper
            uiTertiary = Color(0xFFB87333), // Dark copper
            textPrimary = Color(0xFFFFF8F0), // Warm white
            textSecondary = Color(0xFFE0D5C7), // Warm light gray
            shadowColor = Color(0xFF000000),
            overlayColor = Color(0xFF000000),
        )

    /**
     * Royal Purple Theme
     * Rich purple with gold accents
     * Luxurious and majestic, regal appearance
     * Bold and distinctive for special occasions
     */
    val RoyalPurple =
        ColorTheme(
            id = "royal_purple",
            name = "Royal Purple",
            description = "Majestic purple with luxurious gold accents",
            // Bright gold accents for luxury
            primaryAccent = Color(0xFFFFD93D), // Bright gold (TV optimized)
            primaryAccentLight = Color(0xFFFFEA70), // Light gold
            primaryAccentDark = Color(0xFFF9A825), // Deep gold
            // Rich purple surfaces
            surfacePrimary = Color(0xFF2C1A40), // Deep purple
            surfaceSecondary = Color(0xFF3E2555), // Medium purple
            surfaceTertiary = Color(0xFF533574), // Light purple
            surfaceDark = Color(0xFF1A0D2E), // Darkest purple
            surfaceCenter = Color(0xFF2A1B3D), // Center purple
            secondaryAccent = Color(0xFFF0E6FF), // Very light lavender
            // Purple/magenta prayer indicators with high contrast (match royal theme)
            azanTime = Color(0xFFE040FB), // Bright magenta/pink (Material Purple A200)
            iqamahTime = Color(0xFF7C4DFF), // Deep vibrant purple (Material Deep Purple A200) - more distinct
            sunriseTime = Color(0xFFFF9100), // Bright amber (sunrise warm color)
            // Next prayer highlighting colors (even more vibrant for emphasis)
            nextAzanTime = Color(0xFFFF6BFF), // Ultra bright magenta/pink
            nextIqamahTime = Color(0xFFA070FF), // Brighter vibrant purple
            nextSunriseTime = Color(0xFFFFAB40), // Very bright amber
            backgroundMain = Color(0xFF000000), // Pure black
            surfaceCard = Color(0xFF1A1A1A), // Dark gray
            cardBackground = Color(0xFF252525), // Elevated dark gray
            // Purple UI elements (match prayer indicators)
            uiPrimary = Color(0xFF9575CD), // Light purple (Material Deep Purple 300)
            uiSecondary = Color(0xFFB39DDB), // Very light purple (Material Deep Purple 200)
            uiTertiary = Color(0xFF7E57C2), // Medium purple (Material Deep Purple 400)
            textPrimary = Color(0xFFFFFFFF), // Pure white
            textSecondary = Color(0xFFE0D0F0), // Lavender tinted gray
            shadowColor = Color(0xFF000000),
            overlayColor = Color(0xFF000000),
        )

    /**
     * Ocean Teal Theme
     * Calming teal with pearl white accents
     * Fresh and tranquil, ocean-inspired
     * Refreshing and modern for contemporary mosques
     */
    val OceanTeal =
        ColorTheme(
            id = "ocean_teal",
            name = "Ocean Teal",
            description = "Serene teal with pearl white accents",
            // Bright pearl white accents
            primaryAccent = Color(0xFFF5F5F5), // Very bright pearl
            primaryAccentLight = Color(0xFFFFFFFF), // Pure white
            primaryAccentDark = Color(0xFFE0E0E0), // Light gray
            // Vibrant teal surfaces
            surfacePrimary = Color(0xFF006064), // Deep teal (Material Cyan 900)
            surfaceSecondary = Color(0xFF00838F), // Medium teal (Material Cyan 700)
            surfaceTertiary = Color(0xFF00ACC1), // Light teal (Material Cyan 600)
            surfaceDark = Color(0xFF004D40), // Darkest teal (Material Teal 900)
            surfaceCenter = Color(0xFF00695C), // Center teal (Material Teal 800)
            secondaryAccent = Color(0xFFE0F7FA), // Very light cyan
            // Ocean teal/cyan prayer indicators (match ocean theme perfectly)
            azanTime = Color(0xFF26C6DA), // Bright teal (Material Cyan 400)
            iqamahTime = Color(0xFF4DD0E1), // Light cyan (Material Cyan 300)
            sunriseTime = Color(0xFFFFAB40), // Bright orange (sunrise warm color)
            // Next prayer highlighting colors (brighter ocean tones)
            nextAzanTime = Color(0xFF4DD8EC), // Very bright teal
            nextIqamahTime = Color(0xFF80DEEA), // Ultra light cyan
            nextSunriseTime = Color(0xFFFFCC66), // Very bright orange
            backgroundMain = Color(0xFF000000), // Pure black
            surfaceCard = Color(0xFF1A1A1A), // Dark gray
            cardBackground = Color(0xFF242424), // Elevated dark gray
            // Teal/cyan UI elements (match prayer indicators)
            uiPrimary = Color(0xFF26C6DA), // Bright teal (Material Cyan 400)
            uiSecondary = Color(0xFF4DD0E1), // Light cyan (Material Cyan 300)
            uiTertiary = Color(0xFF00ACC1), // Medium teal (Material Cyan 600)
            textPrimary = Color(0xFFFFFFFF), // Pure white
            textSecondary = Color(0xFFD0F0F0), // Cyan-tinted white
            shadowColor = Color(0xFF000000),
            overlayColor = Color(0xFF000000),
        )

    /**
     * Elegant Charcoal Theme
     * Modern dark gray with rose gold accents
     * Contemporary and sophisticated, minimalist design
     * Neutral and professional for modern spaces
     */
    val ElegantCharcoal =
        ColorTheme(
            id = "elegant_charcoal",
            name = "Elegant Charcoal",
            description = "Modern charcoal with rose gold accents",
            // Bright rose gold accents
            primaryAccent = Color(0xFFF5C4A6), // Bright rose gold (TV optimized)
            primaryAccentLight = Color(0xFFFFDCCC), // Very light rose gold
            primaryAccentDark = Color(0xFFE5A585), // Medium rose gold
            // Modern charcoal surfaces
            surfacePrimary = Color(0xFF2B2B2B), // Deep charcoal
            surfaceSecondary = Color(0xFF3A3A3A), // Medium charcoal
            surfaceTertiary = Color(0xFF4A4A4A), // Light charcoal
            surfaceDark = Color(0xFF1C1C1C), // Darkest charcoal
            surfaceCenter = Color(0xFF2A2A2A), // Center charcoal
            secondaryAccent = Color(0xFFFAFAFA), // Very light gray
            // Neutral rose gold/coral prayer indicators (match elegant theme)
            azanTime = Color(0xFFFF9E80), // Bright coral (warm neutral)
            iqamahTime = Color(0xFFFFB4A0), // Light coral/peach
            sunriseTime = Color(0xFFFF8A65), // Deep coral (sunrise warm)
            // Next prayer highlighting colors (brighter coral/peach)
            nextAzanTime = Color(0xFFFFB89D), // Very bright coral
            nextIqamahTime = Color(0xFFFFCBB3), // Ultra light coral/peach
            nextSunriseTime = Color(0xFFFFAA80), // Very bright coral/orange
            backgroundMain = Color(0xFF000000), // Pure black
            surfaceCard = Color(0xFF1A1A1A), // Dark gray
            cardBackground = Color(0xFF242424), // Elevated dark gray
            // Neutral warm gray/rose gold UI elements (match elegant theme)
            uiPrimary = Color(0xFFE6B8A2), // Rose gold (matches primary accent)
            uiSecondary = Color(0xFFF4D7C9), // Light rose gold
            uiTertiary = Color(0xFFD19B7E), // Dark rose gold
            textPrimary = Color(0xFFFFFFFF), // Pure white
            textSecondary = Color(0xFFE0E0E0), // Very bright gray
            shadowColor = Color(0xFF000000),
            overlayColor = Color(0xFF000000),
        )

    /**
     * Emerald Garden Theme
     * Rich emerald green with gold accents
     * Vibrant and fresh, inspired by lush gardens
     * Perfect for daytime mosque displays
     */
    val EmeraldGarden =
        ColorTheme(
            id = "emerald_garden",
            name = "Emerald Garden",
            description = "Vibrant emerald green with golden highlights",
            // Bright gold accents
            primaryAccent = Color(0xFFFFD700), // Pure gold
            primaryAccentLight = Color(0xFFFFE55C), // Light gold
            primaryAccentDark = Color(0xFFDAA520), // Dark gold
            // Rich emerald surfaces
            surfacePrimary = Color(0xFF0F5C3C), // Deep emerald
            surfaceSecondary = Color(0xFF1B7A52), // Medium emerald
            surfaceTertiary = Color(0xFF27976B), // Light emerald
            surfaceDark = Color(0xFF094029), // Darkest emerald
            surfaceCenter = Color(0xFF126744), // Center emerald
            secondaryAccent = Color(0xFFF0FFF4), // Very light mint
            // Bright green prayer indicators
            azanTime = Color(0xFF4ADE80), // Bright green
            iqamahTime = Color(0xFF22D3EE), // Bright cyan
            sunriseTime = Color(0xFFFBBF24), // Bright amber
            // Next prayer colors (extra bright)
            nextAzanTime = Color(0xFF6EE7A8), // Very bright green
            nextIqamahTime = Color(0xFF67E8F9), // Very bright cyan
            nextSunriseTime = Color(0xFFFCD34D), // Very bright amber
            backgroundMain = Color(0xFF000000), // Pure black
            surfaceCard = Color(0xFF1A1A1A), // Dark gray
            cardBackground = Color(0xFF242424), // Elevated dark gray
            // Golden-green UI elements
            uiPrimary = Color(0xFF34D399), // Emerald green
            uiSecondary = Color(0xFF6EE7B7), // Light emerald
            uiTertiary = Color(0xFF10B981), // Dark emerald
            textPrimary = Color(0xFFFFFFFF), // Pure white
            textSecondary = Color(0xFFD1FAE5), // Mint tinted white
            shadowColor = Color(0xFF000000),
            overlayColor = Color(0xFF000000),
        )

    /**
     * Burgundy Elegance Theme
     * Deep burgundy red with silver accents
     * Sophisticated and warm, traditional appearance
     * Perfect for formal mosque settings
     */
    val BurgundyElegance =
        ColorTheme(
            id = "burgundy_elegance",
            name = "Burgundy Elegance",
            description = "Deep burgundy red with elegant silver accents",
            // Bright silver accents
            primaryAccent = Color(0xFFE5E5E5), // Bright silver
            primaryAccentLight = Color(0xFFF5F5F5), // Very light silver
            primaryAccentDark = Color(0xFFC0C0C0), // Medium silver
            // Rich burgundy surfaces
            surfacePrimary = Color(0xFF5C1A1A), // Deep burgundy
            surfaceSecondary = Color(0xFF7A2626), // Medium burgundy
            surfaceTertiary = Color(0xFF993333), // Light burgundy
            surfaceDark = Color(0xFF3D0F0F), // Darkest burgundy
            surfaceCenter = Color(0xFF4A1616), // Center burgundy
            secondaryAccent = Color(0xFFFFF5F5), // Very light rose
            // Warm red/orange prayer indicators
            azanTime = Color(0xFFF87171), // Bright red
            iqamahTime = Color(0xFFFFB74D), // Bright orange
            sunriseTime = Color(0xFFFF9800), // Deep orange
            // Next prayer colors (extra bright)
            nextAzanTime = Color(0xFFFF8A8A), // Very bright red
            nextIqamahTime = Color(0xFFFFCC80), // Very bright orange
            nextSunriseTime = Color(0xFFFFB74D), // Very bright deep orange
            backgroundMain = Color(0xFF000000), // Pure black
            surfaceCard = Color(0xFF1A1A1A), // Dark gray
            cardBackground = Color(0xFF242424), // Elevated dark gray
            // Warm burgundy UI elements
            uiPrimary = Color(0xFFE57373), // Light burgundy/red
            uiSecondary = Color(0xFFEF9A9A), // Very light red
            uiTertiary = Color(0xFFD32F2F), // Medium red
            textPrimary = Color(0xFFFFFFFF), // Pure white
            textSecondary = Color(0xFFFFE6E6), // Rose-tinted white
            shadowColor = Color(0xFF000000),
            overlayColor = Color(0xFF000000),
        )

    /**
     * Sapphire Night Theme
     * Deep sapphire blue with pearl white accents
     * Cool and calming, evening-optimized
     * Perfect for night prayers
     */
    val SapphireNight =
        ColorTheme(
            id = "sapphire_night",
            name = "Sapphire Night",
            description = "Deep sapphire blue with pearl white elegance",
            // Bright pearl white accents
            primaryAccent = Color(0xFFF8F9FA), // Pearl white
            primaryAccentLight = Color(0xFFFFFFFF), // Pure white
            primaryAccentDark = Color(0xFFE9ECEF), // Light gray
            // Deep sapphire surfaces
            surfacePrimary = Color(0xFF0C1E42), // Deep sapphire
            surfaceSecondary = Color(0xFF162E5A), // Medium sapphire
            surfaceTertiary = Color(0xFF234073), // Light sapphire
            surfaceDark = Color(0xFF081129), // Darkest sapphire
            surfaceCenter = Color(0xFF0E1F3E), // Center sapphire
            secondaryAccent = Color(0xFFF0F4F8), // Very light blue-tint
            // Cool blue prayer indicators
            azanTime = Color(0xFF60A5FA), // Bright blue
            iqamahTime = Color(0xFF93C5FD), // Light blue
            sunriseTime = Color(0xFFFBBF24), // Bright amber
            // Next prayer colors (extra bright)
            nextAzanTime = Color(0xFF93C5FD), // Very bright blue
            nextIqamahTime = Color(0xFFBFDBFE), // Ultra light blue
            nextSunriseTime = Color(0xFFFCD34D), // Very bright amber
            backgroundMain = Color(0xFF000000), // Pure black
            surfaceCard = Color(0xFF0A0F1C), // Dark sapphire-tint
            cardBackground = Color(0xFF141B2E), // Elevated sapphire-tint
            // Cool blue UI elements
            uiPrimary = Color(0xFF3B82F6), // Blue
            uiSecondary = Color(0xFF60A5FA), // Light blue
            uiTertiary = Color(0xFF2563EB), // Dark blue
            textPrimary = Color(0xFFFFFFFF), // Pure white
            textSecondary = Color(0xFFDCE7F5), // Blue-tinted white
            shadowColor = Color(0xFF000000),
            overlayColor = Color(0xFF000000),
        )

    /**
     * Amber Sunset Theme
     * Warm amber and orange with cream accents
     * Warm and welcoming, sunset-inspired
     * Perfect for evening displays
     */
    val AmberSunset =
        ColorTheme(
            id = "amber_sunset",
            name = "Amber Sunset",
            description = "Warm amber sunset with cream highlights",
            // Cream accents
            primaryAccent = Color(0xFFFFF8E1), // Light cream
            primaryAccentLight = Color(0xFFFFFBF0), // Very light cream
            primaryAccentDark = Color(0xFFFFE082), // Medium cream
            // Warm amber surfaces
            surfacePrimary = Color(0xFF5C3D1F), // Deep amber-brown
            surfaceSecondary = Color(0xFF7A522B), // Medium amber-brown
            surfaceTertiary = Color(0xFF996A38), // Light amber-brown
            surfaceDark = Color(0xFF3D2614), // Darkest amber-brown
            surfaceCenter = Color(0xFF4A3118), // Center amber-brown
            secondaryAccent = Color(0xFFFFF9E6), // Very light amber
            // Warm sunset prayer indicators
            azanTime = Color(0xFFFFA726), // Bright orange
            iqamahTime = Color(0xFFFFB74D), // Light orange
            sunriseTime = Color(0xFFFF7043), // Coral orange
            // Next prayer colors (extra bright)
            nextAzanTime = Color(0xFFFFCC80), // Very bright orange
            nextIqamahTime = Color(0xFFFFD699), // Very light orange
            nextSunriseTime = Color(0xFFFF9575), // Very bright coral
            backgroundMain = Color(0xFF000000), // Pure black
            surfaceCard = Color(0xFF1C1410), // Dark warm gray
            cardBackground = Color(0xFF2A1F18), // Elevated warm gray
            // Warm amber UI elements
            uiPrimary = Color(0xFFFFB74D), // Amber
            uiSecondary = Color(0xFFFFCC80), // Light amber
            uiTertiary = Color(0xFFFFA726), // Dark amber
            textPrimary = Color(0xFFFFFFFF), // Pure white
            textSecondary = Color(0xFFFFE8CC), // Warm-tinted white
            shadowColor = Color(0xFF000000),
            overlayColor = Color(0xFF000000),
        )

    /**
     * Returns all available themes
     */
    fun getAllThemes(): List<ColorTheme> =
        listOf(
            ClassicMosque,
            MidnightBlue,
            DesertSand,
            RoyalPurple,
            OceanTeal,
            ElegantCharcoal,
            EmeraldGarden,
            BurgundyElegance,
            SapphireNight,
            AmberSunset,
        )

    /**
     * Gets a theme by its ID, returns ClassicMosque if not found
     */
    fun getThemeById(id: String): ColorTheme = getAllThemes().find { it.id == id } ?: ClassicMosque
}
