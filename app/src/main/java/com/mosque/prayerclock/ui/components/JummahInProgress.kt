package com.mosque.prayerclock.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.ColorPrimaryAccent
import kotlin.math.min

/**
 * Jumu'ah In Progress Screen
 * Shows during Jumu'ah (Friday) prayer time with a countdown
 *
 * Displays:
 * - Full-screen silent phone reminder image as background
 * - Silent phone text overlay
 * - Jumu'ah prayer title
 * - Countdown showing time remaining until end of Jumu'ah duration
 * - Islamic reminder about Jumu'ah
 */
@Composable
fun JummahInProgress(
    minutesRemaining: Long,
    secondsRemaining: Long,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                ),
                        ),
                ),
    ) {
        // Full-screen background image with slight opacity
        Image(
            painter = painterResource(id = R.drawable.silent_phone),
            contentDescription = "Silent phone reminder background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            alpha = 0.4f, // Subtle background so countdown and text are prominent
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val density = LocalDensity.current
            val availableHeightPx = with(density) { maxHeight.toPx() }
            val availableWidthPx = with(density) { maxWidth.toPx() }

            // Calculate dynamic font sizes
            val titleFontSize =
                with(density) {
                    val heightBasedSize = (availableHeightPx * 0.08f).toSp()
                    val widthBasedSize = (availableWidthPx / 20f).toSp()
                    min(heightBasedSize.value, widthBasedSize.value).coerceIn(32f, 72f).sp
                }

            val countdownDigitSize =
                with(density) {
                    val heightBasedSize = (availableHeightPx * 0.20f).toSp()
                    val numElements = 5f // MM:SS
                    val widthBasedSize = (availableWidthPx / numElements * 0.8f).toSp()
                    min(heightBasedSize.value, widthBasedSize.value).sp
                }

            val reminderTextSize =
                with(density) {
                    val heightBasedSize = (availableHeightPx * 0.05f).toSp()
                    val widthBasedSize = (availableWidthPx / 30f).toSp()
                    min(heightBasedSize.value, widthBasedSize.value).coerceIn(18f, 36f).sp
                }

            val silentPhoneTextSize =
                with(density) {
                    val heightBasedSize = (availableHeightPx * 0.06f).toSp()
                    val widthBasedSize = (availableWidthPx / 25f * 2.0f).toSp()
                    val calculatedSize = min(heightBasedSize.value, widthBasedSize.value)
                    calculatedSize.coerceIn(24f, 54f).sp
                }

            val dynamicSpacing = with(density) { (availableHeightPx * 0.04f).toDp().coerceIn(20.dp, 48.dp) }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Silent Phone Reminder Text (prominent at top)
                Text(
                    text = localizedStringResource(R.string.silent_your_phone),
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = silentPhoneTextSize,
                            fontWeight = FontWeight.Bold,
                            lineHeight = silentPhoneTextSize * 1.1f,
                        ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(dynamicSpacing))

                // Jumu'ah Title
                Text(
                    text = "🕌 ${localizedStringResource(R.string.jummah_in_progress_title)}",
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = with(density) { (titleFontSize.toPx() * 0.04f).toSp() },
                        ),
                    color = ColorPrimaryAccent,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(dynamicSpacing * 0.7f))

                // Countdown Clock
                JummahCountdownClock(
                    minutes = minutesRemaining,
                    seconds = secondsRemaining,
                    digitBoxSize = countdownDigitSize,
                )

                Spacer(modifier = Modifier.height(dynamicSpacing))

                // Reminder Text
                Text(
                    text = localizedStringResource(R.string.jummah_prayer_reminder),
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontSize = reminderTextSize,
                            fontWeight = FontWeight.Medium,
                            lineHeight = reminderTextSize * 1.4f,
                        ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )

                Spacer(modifier = Modifier.height(dynamicSpacing * 0.8f))

                // Salawat in Arabic
                Text(
                    text = localizedStringResource(R.string.salawat_arabic),
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = reminderTextSize * 1.3f,
                            fontWeight = FontWeight.Bold,
                            lineHeight = reminderTextSize * 1.8f,
                        ),
                    color = ColorPrimaryAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun JummahCountdownClock(
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
            color = ColorPrimaryAccent,
        )

        // Seconds (SS) - using shared component
        FlipClockDigitPair(
            value = seconds,
            digitBoxSize = digitBoxSize,
        )
    }
}

