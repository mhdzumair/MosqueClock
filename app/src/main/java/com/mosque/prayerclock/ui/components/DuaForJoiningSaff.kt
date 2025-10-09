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
import com.mosque.prayerclock.ui.LocalEffectiveLanguage
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

            // Get effective language and font scaling
            val effectiveLanguage = LocalEffectiveLanguage.current
            val fontScale = getLanguageFontScale(effectiveLanguage)

            // Calculate dynamic font sizes based on screen size with language-specific scaling
            val arabicFontSize =
                with(density) {
                    val calculatedSize = (availableHeightPx * 0.12f * fontScale).toSp().value
                    calculatedSize.coerceIn(48f, 140f).sp
                }
            val transliterationFontSize =
                with(density) {
                    val calculatedSize = (availableHeightPx * 0.055f * fontScale).toSp().value
                    calculatedSize.coerceIn(28f, 80f).sp
                }
            val meaningFontSize =
                with(density) {
                    val calculatedSize = (availableHeightPx * 0.05f * fontScale).toSp().value
                    calculatedSize.coerceIn(24f, 72f).sp
                }
            val titleFontSize =
                with(density) {
                    val calculatedSize = (availableHeightPx * 0.05f * fontScale).toSp().value
                    calculatedSize.coerceIn(24f, 64f).sp
                }

            // Calculate silent phone text size (larger for prominence)
            val silentPhoneTextSize =
                with(density) {
                    val calculatedSize = (availableHeightPx * 0.06f * fontScale).toSp().value
                    calculatedSize.coerceIn(32f, 80f).sp
                }

            // Dynamic spacing based on screen height (same for all languages)
            val sectionSpacing = with(density) { (availableHeightPx * 0.02f).toDp() }.coerceIn(12.dp, 36.dp)
            val topPadding = with(density) { (availableHeightPx * 0.015f).toDp() }.coerceIn(12.dp, 28.dp)
            val sidePadding = 16.dp
            val bottomPadding = 16.dp
            val textHorizontalPadding = 12.dp

            // Line height multipliers (same for all languages)
            val arabicLineHeight = 1.5f
            val transliterationLineHeight = 1.35f
            val meaningLineHeight = 1.45f
            val titleLineHeight = 1.2f
            val silentPhoneLineHeight = 1.2f

            val scrollState = rememberScrollState()

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = sidePadding, end = sidePadding, top = topPadding, bottom = bottomPadding),
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
                            lineHeight = silentPhoneTextSize * silentPhoneLineHeight,
                        ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = textHorizontalPadding),
                )

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Title
                Text(
                    text = "🕌 ${localizedStringResource(R.string.dua_joining_saff_title)}",
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                            lineHeight = titleFontSize * titleLineHeight,
                        ),
                    color = ColorPrimaryAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = textHorizontalPadding),
                )

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Arabic Text
                Text(
                    text = localizedStringResource(R.string.dua_joining_saff_arabic),
                    style =
                        MaterialTheme.typography.displaySmall.copy(
                            fontSize = arabicFontSize,
                            fontWeight = FontWeight.Bold,
                            lineHeight = arabicFontSize * arabicLineHeight,
                        ),
                    color = ColorPrimaryAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = textHorizontalPadding),
                )

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Transliteration
                Text(
                    text = localizedStringResource(R.string.dua_joining_saff_transliteration),
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontSize = transliterationFontSize,
                            fontWeight = FontWeight.Medium,
                            lineHeight = transliterationFontSize * transliterationLineHeight,
                        ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = textHorizontalPadding),
                )

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Meaning
                Text(
                    text = localizedStringResource(R.string.dua_joining_saff_meaning),
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = meaningFontSize,
                            fontWeight = FontWeight.Normal,
                            lineHeight = meaningFontSize * meaningLineHeight,
                        ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = textHorizontalPadding),
                )
            }
        }
    }
}
