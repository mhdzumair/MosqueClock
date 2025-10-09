package com.mosque.prayerclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.LocalEffectiveLanguage
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.AlphaValues
import com.mosque.prayerclock.ui.theme.ColorPrimaryAccent
import com.mosque.prayerclock.utils.TimeUtils
import kotlin.math.min

@Composable
fun FullScreenCountdown(
    prayerName: String,
    @Suppress("UNUSED_PARAMETER") prayerType: PrayerType,
    isIqamah: Boolean,
    minutes: Long,
    seconds: Long,
    azanTime: String,
    show24Hour: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.background, // Pure black
                                MaterialTheme.colorScheme.surface, // Dark gray
                            ),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val density = LocalDensity.current

            // Get effective language and font scaling
            val effectiveLanguage = LocalEffectiveLanguage.current
            val fontScale = getLanguageFontScale(effectiveLanguage)

            // Calculate available space
            val availableWidthPx = with(density) { maxWidth.toPx() }
            val availableHeightPx = with(density) { maxHeight.toPx() }

            // Calculate dynamic sizes for flip clock digits (30% of height)
            val calculatedDigitSize =
                with(density) {
                    val heightBasedSize = (availableHeightPx * 0.30f).toSp()
                    val numElements = 5f // MM:SS
                    val widthBasedSize = (availableWidthPx / numElements * 0.8f).toSp()
                    min(heightBasedSize.value, widthBasedSize.value).sp
                }

            // Prayer title is 40% of digit size (with language scaling)
            val calculatedTitleFontSize = calculatedDigitSize * 0.4f * fontScale

            // Azan time display is 35% of digit size (with language scaling)
            val calculatedAzanTimeFontSize = calculatedDigitSize * 0.4f * fontScale

            // Dua message is 25% of digit size (with language scaling)
            val calculatedDuaFontSize = calculatedDigitSize * 0.25f * fontScale

            // Dynamic padding and spacing
            val dynamicPadding = with(density) { (availableHeightPx * 0.04f).toDp().coerceIn(24.dp, 64.dp) }
            val titleSpacing = with(density) { (availableHeightPx * 0.05f).toDp().coerceIn(40.dp, 80.dp) }
            val duaSpacing = with(density) { (availableHeightPx * 0.05f).toDp().coerceIn(40.dp, 80.dp) }

            Column(
                modifier = Modifier.fillMaxSize().padding(dynamicPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Prayer name + Azan/Iqamah title (except for Sunrise which doesn't need the label)
                val titleText =
                    if (prayerType == PrayerType.SUNRISE) {
                        prayerName
                    } else {
                        val azanOrIqamah =
                            localizedStringResource(if (isIqamah) R.string.iqamah else R.string.azan)
                        "$prayerName $azanOrIqamah"
                    }

                Text(
                    text = titleText,
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            fontSize = calculatedTitleFontSize,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = with(density) { (calculatedTitleFontSize.toPx() * 0.04f).toSp() },
                        ),
                    color = ColorPrimaryAccent, // Primary accent color
                    textAlign = TextAlign.Center,
                )

                // Show Azan time only when counting down to Azan (not Iqamah)
                if (!isIqamah) {
                    Spacer(modifier = Modifier.height(duaSpacing))

                    Text(
                        text = TimeUtils.formatTimeBasedOnPreference(azanTime, show24Hour),
                        style =
                            MaterialTheme.typography.headlineMedium.copy(
                                fontSize = calculatedAzanTimeFontSize,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = with(density) { (calculatedAzanTimeFontSize.toPx() * 0.04f).toSp() },
                            ),
                        color = com.mosque.prayerclock.ui.theme.ColorAzanTime,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(titleSpacing))

                // Animated flip clock countdown (using shared component)
                FullScreenFlipClock(
                    minutes = minutes,
                    seconds = seconds,
                    digitBoxSize = calculatedDigitSize,
                )

                // Show Dua message only for Iqamah countdown (localized)
                if (isIqamah) {
                    Spacer(modifier = Modifier.height(duaSpacing))

                    Text(
                        text = "🤲 ${localizedStringResource(R.string.best_time_dua)}",
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontSize = calculatedDuaFontSize,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = with(density) { (calculatedDuaFontSize.toPx() * 0.05f).toSp() },
                            ),
                        color = ColorPrimaryAccent.copy(alpha = AlphaValues.STRONG), // Primary accent color
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenFlipClock(
    minutes: Long,
    seconds: Long,
    digitBoxSize: TextUnit = 64.sp,
) {
    val density = LocalDensity.current

    // Calculate spacing proportional to digit size
    val spacing = with(density) { (digitBoxSize.toPx() * 0.15f).toDp() }
    val colonSize = digitBoxSize * 0.70f

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Minutes (MM) - using shared component
        FlipClockDigitPair(
            value = minutes,
            digitBoxSize = digitBoxSize,
        )

        // Colon separator
        Text(
            text = ":",
            style =
                MaterialTheme.typography.displayLarge.copy(
                    fontSize = colonSize,
                    fontWeight = FontWeight.Bold,
                ),
            color = ColorPrimaryAccent, // Primary accent color
        )

        // Seconds (SS) - using shared component
        FlipClockDigitPair(
            value = seconds,
            digitBoxSize = digitBoxSize,
        )
    }
}
