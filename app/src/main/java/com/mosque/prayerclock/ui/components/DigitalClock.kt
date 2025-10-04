package com.mosque.prayerclock.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.ui.localizedStringArrayResource
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.AlphaValues
import com.mosque.prayerclock.ui.theme.ColorPrimaryAccent
import com.mosque.prayerclock.utils.LocaleManager
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.min

@Composable
fun DigitalClock(
    show24Hour: Boolean = false,
    showSeconds: Boolean = true,
    modifier: Modifier = Modifier,
    hijriDateRepository: com.mosque.prayerclock.data.repository.HijriDateRepository? = null,
    currentTime: Instant? = null,
) {
    // Use provided time or fallback to local time source
    val timeToUse =
        currentTime ?: run {
            var localCurrentTime by remember { mutableStateOf(Clock.System.now()) }
            LaunchedEffect(Unit) {
                while (true) {
                    localCurrentTime = Clock.System.now()
                    delay(1000)
                }
            }
            localCurrentTime
        }

    val localDateTime = timeToUse.toLocalDateTime(TimeZone.currentSystemDefault())

    val date = Date(timeToUse.toEpochMilliseconds())

    // Get Hijri date from repository or fallback
    var hijriDate by remember {
        mutableStateOf<com.mosque.prayerclock.data.repository.HijriDateRepository.HijriDate?>(null)
    }

    LaunchedEffect(localDateTime.date) {
        hijriDate = hijriDateRepository?.getCurrentHijriDate()
            ?: com.mosque.prayerclock.data.repository.HijriDateRepository
                .HijriDate(7, 2, 1447) // Default fallback
    }

    val hijriMonthNames = localizedStringArrayResource(R.array.hijri_months)

    // Helper to get current locale for date formatting
    val currentLocale =
        LocaleManager.getCurrentLocale(
            com.mosque.prayerclock.ui.LocalLocalizedContext.current,
        )

    // Format Gregorian month and year for the cards
    val monthYear = SimpleDateFormat("MMMM yyyy", currentLocale).format(date)

    // Format time manually for better 12/24 hour control
    val formattedTime =
        if (show24Hour) {
            val timeStr = String.format("%02d:%02d", localDateTime.hour, localDateTime.minute)
            if (showSeconds) "$timeStr:${String.format("%02d", localDateTime.second)}" else timeStr
        } else {
            val hour12 =
                if (localDateTime.hour == 0) {
                    12
                } else if (localDateTime.hour > 12) {
                    localDateTime.hour - 12
                } else {
                    localDateTime.hour
                }
            val ampm =
                if (localDateTime.hour <
                    12
                ) {
                    localizedStringResource(R.string.am)
                } else {
                    localizedStringResource(R.string.pm)
                }
            val timeStr = String.format("%d:%02d", hour12, localDateTime.minute)
            val timeWithSeconds =
                if (showSeconds) {
                    "$timeStr:${String.format(
                        "%02d",
                        localDateTime.second,
                    )}"
                } else {
                    timeStr
                }
            "$timeWithSeconds $ampm"
        }

    // Use BoxWithConstraints to dynamically calculate optimal font size based on available space
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val density = LocalDensity.current

        // Calculate available space
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val availableHeightPx = with(density) { maxHeight.toPx() }

        // Calculate dynamic font size based on available space
        // Similar to analog clock which uses size.minDimension * 0.5f for radius
        val calculatedFontSize =
            with(density) {
                // Time display should take about 70% of available height for maximum visibility
                // (The Column uses SpaceEvenly arrangement, so we need to account for date cards too)
                val heightBasedSize = (availableHeightPx * 0.85f).toSp()

                // Also consider width - time string length varies (12hr format is longer)
                // Approximate: 8 chars for "HH:MM:SS" or 14 chars for "HH:MM:SS AM/PM"
                // Use an even more aggressive multiplier (2.4) to maximize text size
                val estimatedTimeChars = if (show24Hour) 12f else 14f
                val widthBasedSize = (availableWidthPx / estimatedTimeChars * 2.42f).toSp()

                // Use the smaller of the two to ensure it fits
                min(heightBasedSize.value, widthBasedSize.value).sp
            }

        // Calculate dynamic padding for date cards based on available space
        val calculatedCardPadding =
            with(density) {
                // Use smaller padding to make cards more compact
                val dynamicPadding = (availableHeightPx * 0.01f).toDp()
                // Constrain between 4dp and 10dp for more compact cards
                dynamicPadding.coerceIn(3.dp, 8.dp)
            }

        // Clean, elegant digital clock without outer card (already inside ClockSection card)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly, // Even distribution of space
        ) {
            // Clean time display with elegant brass color
            Text(
                text = formattedTime,
                style =
                    MaterialTheme.typography.displayLarge.copy(
                        fontSize = calculatedFontSize,
                        fontWeight = FontWeight.Bold,
                        shadow =
                            Shadow(
                                color =
                                    ColorPrimaryAccent
                                        .copy(alpha = AlphaValues.SUBTLE),
                                offset = Offset(0f, 2f),
                                blurRadius = 8f,
                            ),
                    ),
                color = ColorPrimaryAccent, // Primary accent color
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )

            // Side-by-side date cards layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Hijri Date Card (Left)
                hijriDate?.let { hDate ->
                    val hijriMonth = hijriMonthNames.getOrNull(hDate.month - 1) ?: hijriMonthNames[0]

                    AnimatedContent(
                        targetState = hijriMonth to hDate,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(80, easing = LinearEasing)) togetherWith
                                fadeOut(animationSpec = tween(40, easing = LinearEasing))
                        },
                        label = "hijri_date_transition",
                        modifier = Modifier.weight(1f),
                    ) { (animatedMonth, animatedDate) ->
                        Card(
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
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
                                text = animatedMonth,
                                style =
                                    MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = calculatedFontSize * 0.3f,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )

                            Text(
                                text = animatedDate.day.toString(),
                                style =
                                    MaterialTheme.typography.displayLarge.copy(
                                        fontSize = calculatedFontSize * 0.55f,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )

                            Text(
                                text = animatedDate.year.toString(),
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

                // Gregorian Date Card (Right)
                AnimatedContent(
                    targetState = localDateTime.dayOfMonth to monthYear,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(80, easing = LinearEasing)) togetherWith
                            fadeOut(animationSpec = tween(40, easing = LinearEasing))
                    },
                    label = "gregorian_date_transition",
                    modifier = Modifier.weight(1f),
                ) { (dayOfMonth, monthYearStr) ->
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
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
                            text = monthYearStr.split(" ")[0], // Month name
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
                            text = dayOfMonth.toString(),
                            style =
                                MaterialTheme.typography.displayLarge.copy(
                                    fontSize = calculatedFontSize * 0.55f,
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )

                        Text(
                            text = monthYearStr.split(" ")[1], // Year
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
