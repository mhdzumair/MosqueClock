package com.mosque.prayerclock.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.ui.components.clockfaces.ClockDisplayData
import com.mosque.prayerclock.ui.theme.AlphaValues
import com.mosque.prayerclock.ui.theme.ColorPrimaryAccent
import kotlin.math.min

/**
 * Pure rendering composable for the classic digital clock face.
 * Performs no data processing — all display values come from [ClockDisplayData].
 * Use [rememberClockDisplayData] in the call site to produce the data.
 */
@Composable
fun DigitalClock(
    data: ClockDisplayData,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val density = LocalDensity.current
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val availableHeightPx = with(density) { maxHeight.toPx() }

        val calculatedFontSize =
            with(density) {
                // Guard against unbounded height (e.g. when parent wraps content).
                // In that case, use half the available width as a height proxy so the
                // height cap is meaningful on widescreen TV layouts.
                val safeHeightPx = if (availableHeightPx.isInfinite()) availableWidthPx * 0.5f else availableHeightPx
                // 0.40 allocates ~40% of height to the time row; date cards occupy the rest.
                val heightBasedSize = (safeHeightPx * 0.36f).toSp()
                // 1.9 matches the old visual size (2.42/14 chars = 0.173 per unit width;
                // 1.9/11 actual chars = 0.173 per unit width) without AM/PM overflow.
                val charCount = data.formattedTime.length.toFloat().coerceAtLeast(1f)
                val widthBasedSize = (availableWidthPx / charCount * 1.9f).toSp()
                min(heightBasedSize.value, widthBasedSize.value).sp
            }

        val calculatedCardPadding =
            with(density) {
                val dynamicPadding = (availableHeightPx * 0.01f).toDp()
                dynamicPadding.coerceIn(3.dp, 8.dp)
            }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                text = data.formattedTime,
                style =
                    MaterialTheme.typography.displayLarge.copy(
                        fontSize = calculatedFontSize,
                        fontWeight = FontWeight.Bold,
                        shadow =
                            Shadow(
                                color = ColorPrimaryAccent.copy(alpha = AlphaValues.SUBTLE),
                                offset = Offset(0f, 2f),
                                blurRadius = 8f,
                            ),
                    ),
                color = ColorPrimaryAccent,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Hijri Date Card
                AnimatedContent(
                    targetState = data.hijriMonthName to data.hijriDay,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(80, easing = LinearEasing)) togetherWith
                            fadeOut(animationSpec = tween(40, easing = LinearEasing))
                    },
                    label = "hijri_date_transition",
                    modifier = Modifier.weight(1f),
                ) { (monthName, day) ->
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(calculatedCardPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = monthName,
                                style =
                                    MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = calculatedFontSize * 0.25f,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = day.toString(),
                                style =
                                    MaterialTheme.typography.displayLarge.copy(
                                        fontSize = calculatedFontSize * 0.55f,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = data.hijriYear.toString(),
                                style =
                                    MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = calculatedFontSize * 0.28f,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // Gregorian Date Card
                AnimatedContent(
                    targetState = data.gregorianDay to data.gregorianMonthName,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(80, easing = LinearEasing)) togetherWith
                            fadeOut(animationSpec = tween(40, easing = LinearEasing))
                    },
                    label = "gregorian_date_transition",
                    modifier = Modifier.weight(1f),
                ) { (day, monthName) ->
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(calculatedCardPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = monthName,
                                style =
                                    MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = calculatedFontSize * 0.3f,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                            Text(
                                text = day.toString(),
                                style =
                                    MaterialTheme.typography.displayLarge.copy(
                                        fontSize = calculatedFontSize * 0.55f,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = data.gregorianYear.toString(),
                                style =
                                    MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = calculatedFontSize * 0.28f,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
