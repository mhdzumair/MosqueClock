package com.mosque.prayerclock.ui.components.screenfaces

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.components.TvAutoSizeText
import com.mosque.prayerclock.ui.components.TvTextScale
import com.mosque.prayerclock.ui.components.getCountdownData
import com.mosque.prayerclock.ui.components.rememberTvTextScale
import com.mosque.prayerclock.ui.components.screenfaces.shared.*
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.AppFonts
import com.mosque.prayerclock.ui.theme.LocalColorTheme
import com.mosque.prayerclock.utils.TimeUtils
import com.mosque.prayerclock.viewmodel.MainUiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI

object UfuqScreenFace : ScreenFace {
    override val id = "ufuq"
    override val name = "Ufuq"
    override val description = "Horizon timeline with a 12-hour analog dial"
    override val recommendedColorThemeId = "horizon_dusk"

    @Composable override fun Dashboard(
        data: ScreenDisplayData,
        callbacks: ScreenFaceCallbacks,
        modifier: Modifier,
    ) = UfuqDashboard(data, callbacks, modifier)

    @Composable override fun Countdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) = UfuqCountdown(data, countdown, modifier)

    @Composable override fun FridayNightBayanCountdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) = UfuqCountdown(data, countdown, modifier)

    @Composable override fun Dua(
        data: ScreenDisplayData,
        modifier: Modifier,
    ) = UfuqDua(modifier)

    @Composable override fun FridayBayan(
        data: ScreenDisplayData,
        bayan: JummahData,
        modifier: Modifier,
    ) = UfuqBayan(data, bayan, modifier)
}

@Composable private fun UfuqDashboard(
    data: ScreenDisplayData,
    callbacks: ScreenFaceCallbacks,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain)) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize()) {
            UfuqHeader(data, Modifier.fillMaxWidth().height(s.heightDp(.25f)))
            when (val state = data.mainState) {
                is MainUiState.Success -> {
                    val rows = prayerRows(state.prayerTimes, data.currentTime)
                    val next = resolveDisplayNextPrayer(rows, state.nextPrayer, data.currentTime, state.nextDayFajr)
                    if (data.isCountdownVisibleForWeight) {
                        UfuqAzanHour(
                            data,
                            next,
                            Modifier.weight(.88f).fillMaxWidth().clickable(onClick = callbacks.onOpenSettings),
                        )
                    } else {
                        UfuqPrimary(
                            data,
                            next,
                            Modifier.weight(.55f).fillMaxWidth().clickable(onClick = callbacks.onOpenSettings),
                        )
                        UfuqRail(data, rows, next.type, Modifier.weight(.20f).fillMaxWidth())
                    }
                }

                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TvAutoSizeText(data.errorMessage ?: "…", c.textPrimary, s.height(.08f))
                    }
                }
            }
        }
    }
}

@Composable private fun UfuqHeader(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val h = headerData(data)
    BoxWithConstraints(modifier.background(Brush.verticalGradient(listOf(c.surfacePrimary, c.backgroundMain)))) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = s.widthDp(.025f), vertical = s.heightDp(.10f)),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                TvAutoSizeText(
                    h.mosque.uppercase(),
                    c.textPrimary.copy(alpha = .78f),
                    s.height(.20f),
                    s.height(.12f),
                    Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = s.height(.012f),
                    maxLines = 1,
                )
                if (h.weatherAvailable) {
                    UfuqWeatherBlock(h, s, Modifier.weight(1f).padding(start = s.widthDp(.015f)))
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                TvAutoSizeText(
                    h.hijri,
                    c.textPrimary,
                    s.height(.20f),
                    s.height(.11f),
                    Modifier.weight(1f),
                    maxLines = 1,
                )
                TvAutoSizeText(
                    "${h.weekday} · ${h.gregorian}",
                    c.textPrimary.copy(alpha = .82f),
                    s.height(.17f),
                    s.height(.10f),
                    Modifier
                        .weight(1.35f)
                        .padding(start = s.widthDp(.02f)),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable private fun UfuqWeatherBlock(
    h: FaceHeaderData,
    s: TvTextScale,
    modifier: Modifier = Modifier,
) {
    val c = LocalColorTheme.current
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(s.widthDp(.006f), Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UfuqWeatherIcon(
            h,
            c.textSecondary,
            Modifier.size(s.heightDp(.24f)).clip(RoundedCornerShape(8.dp)),
        )
        Column(Modifier.width(s.widthDp(.135f)), horizontalAlignment = Alignment.Start) {
            TvAutoSizeText(
                h.temperature,
                c.textPrimary,
                s.height(.25f),
                s.height(.15f),
                fontFamily = AppFonts.Display,
                textAlign = TextAlign.Start,
                maxLines = 1,
            )
            if (h.condition.isNotBlank()) {
                TvAutoSizeText(
                    h.condition,
                    c.textSecondary,
                    s.height(.075f),
                    s.height(.045f),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                )
            }
        }
        UfuqWeatherDetails(
            h,
            c.textSecondary,
            c.textPrimary,
            s.height(.085f),
            s.height(.05f),
            Modifier.width(s.widthDp(.19f)),
        )
    }
}

@Composable private fun UfuqWeatherIcon(
    h: FaceHeaderData,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
) {
    val iconUrl = h.weatherIcon?.let { if (it.startsWith("http")) it else if (it.startsWith("//")) "https:$it" else null }
    if (iconUrl != null) {
        val context = LocalContext.current
        val request = remember(context, iconUrl) {
            ImageRequest.Builder(context).data(iconUrl).crossfade(true).build()
        }
        AsyncImage(
            model = request,
            contentDescription = h.condition,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        Icon(Icons.Filled.Cloud, contentDescription = h.condition, tint = fallbackColor, modifier = modifier)
    }
}

/** Compact, bounded weather rows keep long localized labels inside the right edge of the header. */
@Composable private fun UfuqWeatherDetails(
    h: FaceHeaderData,
    labelColor: Color,
    valueColor: Color,
    maxSize: TextUnit,
    minSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            UfuqWeatherMetric(Icons.Filled.Thermostat, h.feelsLike, labelColor, valueColor, maxSize, minSize, Modifier.weight(1f))
            UfuqWeatherMetric(Icons.Filled.WaterDrop, h.humidity, labelColor, valueColor, maxSize, minSize, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            UfuqWeatherMetric(Icons.Filled.Air, h.wind, labelColor, valueColor, maxSize, minSize, Modifier.weight(1f))
            UfuqWeatherMetric(Icons.Filled.WbSunny, "UV ${h.uv}", labelColor, valueColor, maxSize, minSize, Modifier.weight(1f))
        }
    }
}

@Composable private fun UfuqWeatherMetric(
    icon: ImageVector,
    value: String,
    iconColor: Color,
    valueColor: Color,
    maxSize: TextUnit,
    minSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        TvAutoSizeText(value, valueColor, maxSize, minSize, Modifier.weight(1f).padding(start = 4.dp), maxLines = 1)
    }
}

@Composable private fun UfuqPrimary(
    data: ScreenDisplayData,
    next: PrayerInfo,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val analog =
        data.settings.clockType == ClockType.ANALOG ||
            data.settings.clockType == ClockType.BOTH && (data.clock.minute / 2) % 2 != 0
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Row(
            Modifier.fillMaxSize().padding(horizontal = s.widthDp(.025f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (analog) {
                UfuqDial(data, Modifier.weight(.92f).fillMaxHeight(.94f))
            } else {
                BoxWithConstraints(
                    Modifier.weight(1.55f).fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    val clockS = rememberTvTextScale(maxWidth, maxHeight)
                    Column(Modifier.fillMaxSize()) {
                        FacePanelClock(
                            clock = data.clock,
                            primaryColor = c.textPrimary,
                            mutedColor = c.textSecondary,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            fontWeight = FontWeight.Medium,
                            secondaryScale = .32f,
                            amLabel = localizedStringResource(R.string.am),
                            pmLabel = localizedStringResource(R.string.pm),
                        )
                        Spacer(Modifier.height(clockS.heightDp(.018f)))
                        Box(
                            Modifier.fillMaxWidth().height(clockS.heightDp(.008f)).background(c.primaryAccent),
                        )
                    }
                }
            }
            Column(
                Modifier.weight(1f).fillMaxHeight().padding(start = s.widthDp(.028f)),
                verticalArrangement = Arrangement.Center,
            ) {
                TvAutoSizeText(
                    localizedStringResource(R.string.next_prayer).uppercase(),
                    c.textSecondary,
                    s.height(.10f),
                    s.height(.05f),
                    maxLines = 1,
                )
                TvAutoSizeText(
                    next.name,
                    c.textPrimary,
                    s.height(.22f),
                    s.height(.09f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                )
                TvAutoSizeText(
                    TimeUtils.formatTimeBasedOnPreference(next.azanTime, data.settings.show24HourFormat),
                    c.primaryAccent,
                    s.height(.24f),
                    s.height(.11f),
                    fontFamily = AppFonts.Display,
                    fontWeight = FontWeight.Bold,
                )
                next.iqamahTime?.let {
                    TvAutoSizeText(
                        "${localizedStringResource(
                            R.string.iqamah_letter,
                        )} ${TimeUtils.formatTimeBasedOnPreference(it, data.settings.show24HourFormat)}",
                        c.textSecondary,
                        s.height(.09f),
                        s.height(.045f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Largest Archivo numeral size that fits the assigned region for the current string.
 * Shorter times (e.g. "9:54:18 PM") scale up to fill the same width as "12:51:30 AM".
 */
@Composable private fun UfuqClockText(
    text: String,
    color: Color,
    maxHeightFraction: Float,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Medium,
    lineHeightRatio: Float = 1.12f,
) {
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val heightCapSp = s.fit(maxHeightFraction, 1f, 1.55f).value
        val fontSize = remember(text, widthPx, heightPx, heightCapSp, fontWeight, lineHeightRatio, density, textMeasurer) {
            if (widthPx <= 0f || heightPx <= 0f || heightCapSp <= 0f) {
                return@remember s.fit(maxHeightFraction, text.length.toFloat().coerceAtLeast(1f), 1.55f)
            }
            val style = TextStyle(fontFamily = AppFonts.Display, fontWeight = fontWeight)
            var low = minOf(1f, heightCapSp)
            var high = heightCapSp
            var best = low
            while (high - low > 0.5f) {
                val mid = (low + high) / 2f
                val result = textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = style.copy(
                        fontSize = mid.sp,
                        lineHeight = (mid * lineHeightRatio).sp,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
                val fitsWidth = result.size.width <= widthPx
                val fitsHeight = result.size.height <= heightPx
                if (fitsWidth && fitsHeight) {
                    best = mid
                    low = mid
                } else {
                    high = mid
                }
            }
            best.sp
        }
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            lineHeight = fontSize * lineHeightRatio,
            fontWeight = fontWeight,
            fontFamily = AppFonts.Display,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable private fun UfuqSideClockPanel(
    data: ScreenDisplayData,
    activeLabelRes: Int,
    activeTime: String,
    referenceLabelRes: Int?,
    referenceTime: String?,
    s: TvTextScale,
    modifier: Modifier = Modifier,
) {
    val c = LocalColorTheme.current
    Column(
        modifier,
        verticalArrangement = Arrangement.Bottom,
    ) {
        UfuqClockText(
            data.clock.formattedTime,
            c.textPrimary,
            maxHeightFraction = .22f,
        )
        Spacer(Modifier.height(s.heightDp(.012f)))
        TvAutoSizeText(
            "${localizedStringResource(activeLabelRes)} $activeTime",
            c.textPrimary.copy(alpha = .88f),
            s.height(.06f),
            s.height(.032f),
            fontFamily = AppFonts.Display,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        referenceLabelRes?.let { labelRes ->
            referenceTime?.let { refTime ->
                TvAutoSizeText(
                    "${localizedStringResource(labelRes)} $refTime",
                    c.textSecondary,
                    s.height(.048f),
                    s.height(.026f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable private fun UfuqCompactHeader(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val h = headerData(data)
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvAutoSizeText(
                h.mosque.uppercase(),
                c.textPrimary.copy(alpha = .86f),
                s.height(.03f),
                s.height(.018f),
                Modifier.weight(.42f),
                fontFamily = AppFonts.Display,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = s.height(.008f),
                maxLines = 1,
            )
            TvAutoSizeText(
                buildString {
                    append(h.hijri)
                    append(" · ")
                    append(h.weekday)
                    append(" · ")
                    append(h.gregorian)
                    if (h.weatherAvailable) {
                        append(" · ")
                        append(h.temperature)
                        append(" · ")
                        append(h.humidity)
                    }
                },
                c.textSecondary,
                s.height(.03f),
                s.height(.018f),
                Modifier.weight(.58f),
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
}

@Composable private fun UfuqDial(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val hourAngleTarget = clockHourAngle(data.clock.hour12, data.clock.minute, data.clock.second)
    val minuteAngleTarget = clockMinuteAngle(data.clock.minute, data.clock.second)
    val secondAngleTarget = clockSecondAngle(data.clock.second)
    val hourAngle by rememberClockwiseHandAngle(hourAngleTarget, "ufuq_hour")
    val minuteAngle by rememberClockwiseHandAngle(minuteAngleTarget, "ufuq_minute")
    val secondAngle by rememberClockwiseHandAngle(secondAngleTarget, "ufuq_second")
    val handColor = Color.White
    val tickColor = c.textSecondary.copy(alpha = .80f)
    val numberPaint = remember(handColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = handColor.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val dialSize = minOf(maxWidth, maxHeight)
        Box(Modifier.size(dialSize), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.46f

                drawCircle(c.backgroundMain, radius, center)
                drawCircle(c.textSecondary.copy(alpha = .70f), radius, center, style = Stroke(radius * 0.012f))
                drawCircle(tickColor.copy(alpha = .22f), radius * 0.76f, center, style = Stroke(radius * 0.005f))

                repeat(60) { index ->
                    val angle = index * 6f - 90f
                    val major = index % 5 == 0
                    if (major) {
                        val atQuarter = index % 15 == 0
                        drawLine(
                            color = if (atQuarter) handColor else tickColor.copy(alpha = .75f),
                            start = ufuqPolar(center, radius * 0.84f, angle),
                            end = ufuqPolar(center, radius * 0.94f, angle),
                            strokeWidth = radius * if (atQuarter) 0.028f else 0.018f,
                            cap = StrokeCap.Round,
                        )
                    } else {
                        drawLine(
                            tickColor.copy(alpha = .65f),
                            ufuqPolar(center, radius * 0.88f, angle),
                            ufuqPolar(center, radius * 0.93f, angle),
                            radius * 0.006f,
                            StrokeCap.Round,
                        )
                    }
                }

                numberPaint.textSize = radius * 0.18f
                drawIntoCanvas { canvas ->
                    listOf("12" to -90f, "3" to 0f, "6" to 90f, "9" to 180f).forEach { (label, angle) ->
                        val point = ufuqPolar(center, radius * 0.70f, angle)
                        val baseline = point.y - (numberPaint.ascent() + numberPaint.descent()) / 2f
                        canvas.nativeCanvas.drawText(label, point.x, baseline, numberPaint)
                    }
                }

                drawLine(
                    handColor,
                    ufuqPolar(center, radius * 0.10f, hourAngle + 180f),
                    ufuqPolar(center, radius * 0.48f, hourAngle),
                    radius * 0.050f,
                    StrokeCap.Round,
                )
                drawLine(
                    handColor,
                    ufuqPolar(center, radius * 0.12f, minuteAngle + 180f),
                    ufuqPolar(center, radius * 0.68f, minuteAngle),
                    radius * 0.034f,
                    StrokeCap.Round,
                )
                if (data.clock.showSeconds) {
                    val tipRadius = radius * 0.018f
                    val tipCenter = ufuqPolar(center, radius * 0.80f, secondAngle)
                    drawLine(
                        c.primaryAccent,
                        ufuqPolar(center, radius * 0.16f, secondAngle + 180f),
                        ufuqPolar(center, radius * 0.80f - tipRadius, secondAngle),
                        radius * 0.008f,
                        StrokeCap.Round,
                    )
                    drawCircle(c.primaryAccent, tipRadius, tipCenter)
                }
                drawCircle(handColor, radius * 0.055f, center)
                drawCircle(c.backgroundMain, radius * 0.035f, center)
                drawCircle(c.primaryAccent, radius * 0.016f, center)
            }
        }
    }
}

private fun ufuqPolar(center: Offset, length: Float, degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    return Offset(
        center.x + kotlin.math.cos(radians) * length,
        center.y + kotlin.math.sin(radians) * length,
    )
}

@Composable private fun UfuqAzanHour(
    data: ScreenDisplayData,
    next: PrayerInfo,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val cd = inlineCountdown(data, next)
    val remainingSeconds = cd.minutes * 60 + cd.seconds
    val totalSeconds = if (cd.isIqamah) {
        minutesBetween(next.azanTime, cd.targetTime).coerceAtLeast(1) * 60
    } else {
        3600L
    }
    val progress = (1f - remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    val activeTime = if (cd.isIqamah) next.iqamahTime ?: next.azanTime else next.azanTime
    val referenceTime = if (cd.isIqamah) next.azanTime else next.iqamahTime

    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(
            Modifier.fillMaxSize().padding(horizontal = s.widthDp(.018f)),
            verticalArrangement = Arrangement.Center,
        ) {
            TvAutoSizeText(
                prayerWithEventLabel(next, cd.isIqamah),
                c.primaryAccent,
                s.height(.055f),
                s.height(.03f),
                fontWeight = FontWeight.Bold,
                letterSpacing = s.height(.006f),
                maxLines = 1,
            )
            Row(
                Modifier.fillMaxWidth().height(s.heightDp(.52f)).padding(top = s.heightDp(.01f)),
                verticalAlignment = Alignment.Bottom,
            ) {
                UfuqClockText(
                    "%02d:%02d".format(cd.minutes, cd.seconds),
                    c.primaryAccent,
                    maxHeightFraction = .90f,
                    modifier = Modifier.weight(1.15f),
                )
                if (next.type != PrayerType.SUNRISE) {
                    UfuqSideClockPanel(
                        data = data,
                        activeLabelRes = if (cd.isIqamah) R.string.iqamah else R.string.azan,
                        activeTime = TimeUtils.formatTimeBasedOnPreference(
                            activeTime,
                            data.settings.show24HourFormat,
                        ),
                        referenceLabelRes = if (cd.isIqamah) R.string.azan else R.string.iqamah_letter,
                        referenceTime = referenceTime?.let {
                            TimeUtils.formatTimeBasedOnPreference(it, data.settings.show24HourFormat)
                        },
                        s = s,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = s.widthDp(.04f), bottom = s.heightDp(.03f)),
                    )
                }
            }
            Column(Modifier.padding(top = s.heightDp(.05f))) {
                AnimatedFaceSunProgress(
                    progress,
                    c.surfaceTertiary,
                    c.primaryAccent,
                    Modifier.fillMaxWidth().height(s.heightDp(.048f)),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = s.heightDp(.035f)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    TvAutoSizeText(
                        data.clock.formattedTime,
                        c.textSecondary,
                        s.height(.065f),
                        s.height(.03f),
                        Modifier.weight(1f),
                        fontFamily = AppFonts.Display,
                    )
                    if (next.type != PrayerType.SUNRISE) {
                        TvAutoSizeText(
                            "${localizedStringResource(if (cd.isIqamah) R.string.iqamah else R.string.azan)} " +
                                TimeUtils.formatTimeBasedOnPreference(cd.targetTime, data.settings.show24HourFormat),
                            c.primaryAccent,
                            s.height(.065f),
                            s.height(.03f),
                            Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun UfuqRail(
    data: ScreenDisplayData,
    rows: List<PrayerInfo>,
    next: PrayerType?,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val now = data.currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val nowTimeString = "%02d:%02d".format(now.hour, now.minute)

    // The columns are evenly spaced (1/n each) but the prayers themselves are not evenly spaced in
    // time — Zuhr..Asr is hours longer than Fajr..Sunrise. A single linear fraction across the whole
    // Fajr..Isha range would drift out from under its column by the time it reaches the far side, so
    // the marker is interpolated segment-by-segment instead: it always sits between the boundaries of
    // whichever two columns "now" actually falls between.
    val n = rows.size
    val nowMinute = now.hour * 60 + now.minute + now.second / 60f
    val firstMinute = minutesOfDay(rows.first().azanTime).toFloat()
    val lastMinute = minutesOfDay(rows.last().azanTime).toFloat()
    val marker = when {
        nowMinute <= firstMinute -> .5f / n
        nowMinute >= lastMinute -> (n - .5f) / n
        else -> {
            val nextIndex = rows.indices.first { minutesOfDay(rows[it].azanTime) > nowMinute }
            val prevIndex = nextIndex - 1
            val prevMinute = minutesOfDay(rows[prevIndex].azanTime).toFloat()
            val nextMinute = minutesOfDay(rows[nextIndex].azanTime).toFloat()
            val within = ((nowMinute - prevMinute) / (nextMinute - prevMinute).coerceAtLeast(1f)).coerceIn(0f, 1f)
            ((prevIndex + within + .5f) / n).coerceIn(0f, 1f)
        }
    }

    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = s.widthDp(.02f))
                .padding(top = s.heightDp(.006f), bottom = s.heightDp(.008f)),
        ) {
            val hasIqamahTimes = rows.any { it.iqamahTime != null }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedFaceSunProgress(
                    // Prayer columns occupy 96% of the face width. The mode label reserves 6%
                    // within that row, so map the marker into the remaining 90% rail width.
                    if (hasIqamahTimes) (marker * .96f / .90f).coerceIn(0f, 1f) else marker,
                    c.surfaceTertiary,
                    c.primaryAccent,
                    Modifier.weight(1f).height(s.heightDp(.048f)),
                )
                if (hasIqamahTimes) {
                    AnimatedContent(
                        targetState = data.globalShowAzan,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(800)) togetherWith fadeOut(animationSpec = tween(800))
                        },
                        modifier = Modifier.width(s.widthDp(.06f)).padding(start = s.widthDp(.018f)),
                        label = "ufuq_rail_mode",
                    ) { showAzan ->
                        TvAutoSizeText(
                            localizedStringResource(if (showAzan) R.string.azan_letter else R.string.iqamah_letter),
                            if (showAzan) c.azanTime else c.iqamahTime,
                            s.height(.14f),
                            s.height(.07f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.height(s.heightDp(.028f)))
            Row(Modifier.fillMaxWidth()) {
                rows.forEach { p ->
                    val selected = p.type == next
                    val isPast = !selected && TimeUtils.compareTimeStrings(p.azanTime, nowTimeString) < 0
                    val nameColor = if (selected) c.primaryAccent else c.textSecondary
                    Column(Modifier.weight(1f).padding(horizontal = 4.dp), verticalArrangement = Arrangement.Top) {
                        TvAutoSizeText(
                            p.name,
                            nameColor,
                            s.height(.20f),
                            s.height(.09f),
                            fontWeight = if (selected) FontWeight.Bold else null,
                            maxLines = 2,
                        )
                        if (p.type != PrayerType.SUNRISE && p.iqamahTime != null) {
                            AnimatedContent(
                                targetState = data.globalShowAzan,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(800)) togetherWith fadeOut(animationSpec = tween(800))
                                },
                                label = "ufuq_rail_prayer_mode",
                            ) { showAzan ->
                                TvAutoSizeText(
                                    localizedStringResource(
                                        if (showAzan) R.string.azan_letter else R.string.iqamah_letter,
                                    ),
                                    (if (showAzan) c.azanTime else c.iqamahTime).copy(
                                        alpha = if (isPast) .55f else .85f,
                                    ),
                                    s.height(.12f),
                                    s.height(.06f),
                                    maxLines = 1,
                                )
                            }
                        }
                        AnimatedContent(
                            targetState = data.globalShowAzan,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(800)) togetherWith fadeOut(animationSpec = tween(800))
                            },
                            label = "ufuq_rail_time",
                        ) { showAzan ->
                            val columnShowingAzan = showAzan || p.iqamahTime == null
                            val time = if (columnShowingAzan) p.azanTime else p.iqamahTime ?: p.azanTime
                            val color = when {
                                p.type == PrayerType.SUNRISE -> when {
                                    selected -> c.nextSunriseTime
                                    isPast -> c.textSecondary
                                    else -> c.sunriseTime
                                }
                                columnShowingAzan -> when {
                                    selected -> c.nextAzanTime
                                    isPast -> c.textSecondary
                                    else -> c.azanTime
                                }
                                else -> when {
                                    selected -> c.nextIqamahTime
                                    isPast -> c.textSecondary
                                    else -> c.iqamahTime
                                }
                            }
                            TvAutoSizeText(
                                TimeUtils.formatTimeBasedOnPreference(time, data.settings.show24HourFormat),
                                color,
                                s.height(.24f),
                                s.height(.11f),
                                fontFamily = AppFonts.Display,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun UfuqCountdown(
    data: ScreenDisplayData,
    value: FullScreenCountdownData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain)) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(
            Modifier.fillMaxSize().padding(horizontal = s.widthDp(.057f)),
            verticalArrangement = Arrangement.Center,
        ) {
            UfuqCompactHeader(data, Modifier.fillMaxWidth())
            Spacer(Modifier.height(s.heightDp(.02f)))
            TvAutoSizeText(
                fullScreenCountdownTitle(value.prayerName, value.prayerType, value.isIqamah, value.kind),
                c.primaryAccent,
                s.height(.07f),
                s.height(.04f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Row(
                Modifier.fillMaxWidth().height(s.heightDp(.52f)).padding(top = s.heightDp(.01f)),
                verticalAlignment = Alignment.Bottom,
            ) {
                UfuqClockText(
                    "%02d:%02d".format(value.minutes, value.seconds),
                    c.textPrimary,
                    maxHeightFraction = .90f,
                    modifier = Modifier.weight(1.15f),
                )
                if (value.prayerType != PrayerType.SUNRISE) {
                    UfuqSideClockPanel(
                        data = data,
                        activeLabelRes = if (value.kind == CountdownKind.FRIDAY_NIGHT_BAYAN) R.string.friday_night_bayan
                            else if (value.isIqamah) R.string.iqamah else R.string.azan,
                        activeTime = TimeUtils.formatTimeBasedOnPreference(
                            value.targetTime,
                            data.settings.show24HourFormat,
                        ),
                        referenceLabelRes = null,
                        referenceTime = null,
                        s = s,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = s.widthDp(.04f), bottom = s.heightDp(.03f)),
                    )
                }
            }
            Spacer(Modifier.height(s.heightDp(.02f)))
            AnimatedFaceSunProgress(
                value.elapsedFraction(),
                c.surfaceTertiary,
                c.primaryAccent,
                Modifier.fillMaxWidth().height(s.heightDp(.048f)),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = s.heightDp(.018f)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                TvAutoSizeText(
                    data.clock.formattedTime,
                    c.textSecondary,
                    s.height(.065f),
                    s.height(.03f),
                    Modifier.weight(1f),
                    fontFamily = AppFonts.Display,
                )
                if (value.prayerType != PrayerType.SUNRISE) {
                    TvAutoSizeText(
                        "${localizedStringResource(if (value.kind == CountdownKind.FRIDAY_NIGHT_BAYAN) R.string.friday_night_bayan else if (value.isIqamah) R.string.iqamah else R.string.azan)} " +
                            TimeUtils.formatTimeBasedOnPreference(value.targetTime, data.settings.show24HourFormat),
                        c.primaryAccent,
                        s.height(.12f),
                        s.height(.065f),
                        Modifier.weight(1f),
                        fontFamily = AppFonts.Display,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable private fun UfuqBayan(
    data: ScreenDisplayData,
    bayan: JummahData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(
        modifier.background(Brush.verticalGradient(listOf(c.surfacePrimary, c.backgroundMain))),
    ) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        val totalSeconds = (data.settings.jummahDurationMinutes * 60).coerceAtLeast(1)
        val remainingSeconds = bayan.hoursRemaining * 3600 + bayan.minutesRemaining * 60 + bayan.secondsRemaining
        val progress = (1f - remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
        Column(
            Modifier.fillMaxSize().padding(horizontal = s.widthDp(.018f), vertical = s.heightDp(.035f)),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            TvAutoSizeText(
                localizedStringResource(R.string.silent_your_phone),
                c.primaryAccent,
                s.height(.055f),
                s.height(.028f),
                fontWeight = FontWeight.Bold,
                letterSpacing = s.height(.006f),
                maxLines = 1,
            )
            Column {
                TvAutoSizeText(
                    localizedStringResource(R.string.jummah_in_progress_title),
                    c.textPrimary,
                    s.height(.14f),
                    s.height(.07f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                )
                TvAutoSizeText(
                    bayan.formattedRemaining(),
                    c.primaryAccent,
                    s.height(.34f),
                    s.height(.18f),
                    fontFamily = AppFonts.Display,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            Column {
                AnimatedFaceProgress(
                    progress,
                    c.surfaceTertiary,
                    c.primaryAccent,
                    Modifier.fillMaxWidth().height(s.heightDp(.012f)),
                )
                Spacer(Modifier.height(s.heightDp(.03f)))
                TvAutoSizeText(
                    localizedStringResource(R.string.jummah_prayer_reminder),
                    c.textSecondary,
                    s.height(.055f),
                    s.height(.028f),
                    maxLines = 4,
                )
            }
        }
    }
}

@Composable private fun UfuqDua(modifier: Modifier) {
    val c = LocalColorTheme.current
    BoxWithConstraints(
        modifier.background(Brush.verticalGradient(listOf(c.backgroundMain, c.surfacePrimary))),
    ) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(
            Modifier.fillMaxSize().padding(horizontal = s.widthDp(.018f), vertical = s.heightDp(.035f)),
            verticalArrangement = Arrangement.spacedBy(s.heightDp(.025f)),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                TvAutoSizeText(
                    localizedStringResource(R.string.silent_your_phone),
                    c.primaryAccent,
                    s.height(.05f),
                    s.height(.025f),
                    Modifier.weight(.58f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = s.height(.005f),
                    maxLines = 1,
                )
                Spacer(Modifier.width(s.widthDp(.018f)))
                TvAutoSizeText(
                    localizedStringResource(R.string.dua_joining_saff_title),
                    c.textSecondary,
                    s.height(.04f),
                    s.height(.02f),
                    Modifier.weight(.42f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(c.surfaceTertiary))
            TvAutoSizeText(
                localizedStringResource(R.string.dua_joining_saff_arabic),
                c.textPrimary,
                s.height(.17f),
                s.height(.075f),
                fontFamily = AppFonts.Arabic,
                maxLines = 2,
            )
            TvAutoSizeText(
                localizedStringResource(R.string.dua_joining_saff_transliteration),
                c.primaryAccent,
                s.height(.07f),
                s.height(.035f),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            TvAutoSizeText(
                localizedStringResource(R.string.dua_joining_saff_meaning),
                c.textSecondary,
                s.height(.048f),
                s.height(.024f),
                maxLines = 3,
            )
        }
    }
}
