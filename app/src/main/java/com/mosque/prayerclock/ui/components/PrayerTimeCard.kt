package com.mosque.prayerclock.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.theme.ColorAzanTime
import com.mosque.prayerclock.ui.theme.ColorIqamahTime
import com.mosque.prayerclock.ui.theme.ColorSunriseTime
import com.mosque.prayerclock.ui.theme.ColorNextAzanTime
import com.mosque.prayerclock.ui.theme.ColorNextIqamahTime
import com.mosque.prayerclock.ui.theme.ColorNextSunriseTime
import kotlin.math.min

/**
 * Compact Prayer Time Card Component
 * 
 * Displays prayer name and time with dynamic font scaling.
 * Used in the prayer times row at the bottom of the main screen.
 * 
 * Features:
 * - Dynamic font sizing based on available space
 * - Color-coded prayer times (Azan/Iqamah/Sunrise)
 * - Animated transitions between Azan and Iqamah times
 * - Highlighted border for the next prayer
 */
@Composable
fun PrayerTimeCard(
    prayerInfo: PrayerInfo,
    isNext: Boolean = false,
    show24Hour: Boolean = false,
    globalShowAzan: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val cardPadding = 1.dp
    // Use consistent content padding for all cards to prevent size variations
    val contentPadding = 2.dp
    val cornerRadius = 8.dp

    // Use global synchronized animation state
    // All cards will show Azan/Iqamah at the same time
    val isShowingAzan = globalShowAzan || prayerInfo.iqamahTime == null

    // Get the prayer color based on current state
    // Use theme-specific "next" colors when isNext is true for better emphasis
    val prayerColor = if (prayerInfo.type == PrayerType.SUNRISE) {
        if (isNext) ColorNextSunriseTime else ColorSunriseTime
    } else if (isShowingAzan) {
        if (isNext) ColorNextAzanTime else ColorAzanTime
    } else {
        if (isNext) ColorNextIqamahTime else ColorIqamahTime
    }

    Card(
        modifier =
            modifier
                .padding(cardPadding)
                .let { cardModifier ->
                    if (isNext) {
                        cardModifier.border(
                            width = 2.dp,
                            color = prayerColor,
                            shape = RoundedCornerShape(cornerRadius),
                        )
                    } else {
                        cardModifier
                    }
                },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 6.dp,
            ),
        shape = RoundedCornerShape(cornerRadius),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(2.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isNext) {
                            prayerColor.copy(alpha = 0.05f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        },
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(cornerRadius - 2.dp),
        ) {
            // Use BoxWithConstraints to dynamically calculate font sizes based on card width
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().padding(contentPadding),
            ) {
                val density = LocalDensity.current

                // Calculate available space for this prayer card
                val availableWidthPx = with(density) { maxWidth.toPx() }
                val availableHeightPx = with(density) { maxHeight.toPx() }

                // Calculate dynamic font sizes based on available space
                // Each card gets equal space in the row, so we optimize for that
                val calculatedTimeFontSize =
                    with(density) {
                        // Time takes about 60% of card height
                        val heightBasedSize = (availableHeightPx * 0.60f).toSp()

                        // Time string is typically 7-8 chars ("12:45 AM" or "12:45")
                        val estimatedTimeChars = if (show24Hour) 5f else 8f
                        val widthBasedSize = (availableWidthPx / estimatedTimeChars * 1.8f).toSp()

                        // Use the smaller to ensure it fits
                        min(heightBasedSize.value, widthBasedSize.value).sp
                    }

                // Prayer name should scale independently based on available space
                // It should be smaller than time but maximize available space
                // Special handling for Sunrise which has longer name in some languages
                val calculatedNameFontSize =
                    with(density) {
                        // Name takes about 25% of card height
                        val heightBasedSize = (availableHeightPx * 0.25f).toSp()

                        // Adjust character estimation based on prayer type
                        // Sunrise names are longer in some languages (e.g., Tamil "சூரிய உதயம்")
                        val estimatedNameChars = if (prayerInfo.type == PrayerType.SUNRISE) 12f else 10f
                        val widthMultiplier = if (prayerInfo.type == PrayerType.SUNRISE) 1.6f else 2.0f
                        val widthBasedSize = (availableWidthPx / estimatedNameChars * widthMultiplier).toSp()

                        // Use the smaller to ensure it fits, but cap at a percentage of time size
                        // Sunrise gets a lower cap to ensure it fits on one line
                        val dynamicSize = min(heightBasedSize.value, widthBasedSize.value)
                        val maxAllowed = if (prayerInfo.type == PrayerType.SUNRISE) {
                            calculatedTimeFontSize.value * 0.45f // Smaller for sunrise
                        } else {
                            calculatedTimeFontSize.value * 0.60f // Normal prayers
                        }
                        
                        min(dynamicSize, maxAllowed).sp
                    }
                
                val spacingSize = 1.dp

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = prayerInfo.name,
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontSize = calculatedNameFontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )

                    Spacer(modifier = Modifier.height(spacingSize))

                    // Simplified time display without abbreviation text for better readability
                    AnimatedContent(
                        targetState = globalShowAzan,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(800)) togetherWith
                                fadeOut(animationSpec = tween(800))
                        },
                    ) { isShowingAzan ->
                        Text(
                            text =
                                if (isShowingAzan || prayerInfo.iqamahTime == null) {
                                    formatTimeBasedOnPreference(prayerInfo.azanTime, show24Hour)
                                } else {
                                    formatTimeBasedOnPreference(
                                        prayerInfo.iqamahTime,
                                        show24Hour,
                                    )
                                },
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = calculatedTimeFontSize,
                                    fontWeight = FontWeight.Bold,
                                ),
                            maxLines = 1,
                            softWrap = false,
                            color = prayerColor,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper function to format time based on user preference (12H/24H)
 */
private fun formatTimeBasedOnPreference(
    time: String,
    show24Hour: Boolean,
): String {
    if (show24Hour) {
        return time
    }

    // Convert 24-hour format to 12-hour format
    val parts = time.split(":")
    if (parts.size != 2) return time

    val hour = parts[0].toIntOrNull() ?: return time
    val minute = parts[1]

    return when {
        hour == 0 -> "12:$minute AM"
        hour < 12 -> "$hour:$minute AM"
        hour == 12 -> "12:$minute PM"
        else -> "${hour - 12}:$minute PM"
    }
}
