package com.mosque.prayerclock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.ColorPrimaryAccent
import kotlin.math.min

/**
 * Dua for Joining the Prayer Row (Saff)
 * Shows for a configurable duration after Iqamah time ends (default: 5 minutes)
 *
 * Displays:
 * - Full-screen silent phone reminder image as background
 * - Silent phone text overlay
 * - Arabic text
 * - Transliteration in selected language
 * - Meaning in selected language
 */
@Composable
fun DuaForJoiningSaff(modifier: Modifier = Modifier) {
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
            alpha = 0.4f, // Subtle background so text is still readable
        )
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val density = LocalDensity.current
            val availableHeightPx = with(density) { maxHeight.toPx() }
            val availableWidthPx = with(density) { maxWidth.toPx() }

            // Calculate dynamic font sizes
            val arabicFontSize =
                with(density) {
                    min((availableHeightPx * 0.15f).toSp().value, 48f).sp
                }
            val transliterationFontSize =
                with(density) {
                    min((availableHeightPx * 0.06f).toSp().value, 32f).sp
                }
            val meaningFontSize =
                with(density) {
                    min((availableHeightPx * 0.05f).toSp().value, 28f).sp
                }
            val titleFontSize =
                with(density) {
                    min((availableHeightPx * 0.05f).toSp().value, 24f).sp
                }

            // Calculate silent phone text size (larger for prominence)
            val silentPhoneTextSize =
                with(density) {
                    val heightBasedSize = (availableHeightPx * 0.08f).toSp()
                    val widthBasedSize = (availableWidthPx / 25f * 2.0f).toSp()
                    val calculatedSize = min(heightBasedSize.value, widthBasedSize.value)
                    calculatedSize.coerceIn(24f, 64f).sp
                }

            val scrollState = rememberScrollState()

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = "🕌 ${localizedStringResource(R.string.dua_joining_saff_title)}",
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = ColorPrimaryAccent,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Arabic Text
                Text(
                    text = localizedStringResource(R.string.dua_joining_saff_arabic),
                    style =
                        MaterialTheme.typography.displaySmall.copy(
                            fontSize = arabicFontSize,
                            fontWeight = FontWeight.Bold,
                            lineHeight = arabicFontSize * 1.5f,
                        ),
                    color = ColorPrimaryAccent,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Transliteration
                Text(
                    text = localizedStringResource(R.string.dua_joining_saff_transliteration),
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontSize = transliterationFontSize,
                            fontWeight = FontWeight.Medium,
                            lineHeight = transliterationFontSize * 1.4f,
                        ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Meaning
                Text(
                    text = localizedStringResource(R.string.dua_joining_saff_meaning),
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = meaningFontSize,
                            fontWeight = FontWeight.Normal,
                            lineHeight = meaningFontSize * 1.5f,
                        ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
