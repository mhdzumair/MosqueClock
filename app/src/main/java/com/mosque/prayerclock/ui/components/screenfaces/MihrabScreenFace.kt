package com.mosque.prayerclock.ui.components.screenfaces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.components.*
import com.mosque.prayerclock.ui.components.screenfaces.shared.*
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.AppFonts
import com.mosque.prayerclock.ui.theme.LocalColorTheme
import com.mosque.prayerclock.utils.TimeUtils
import com.mosque.prayerclock.viewmodel.MainUiState
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun mihrabCartoucheShape(taperFraction: Float = 0.4f) =
    GenericShape { size, _ ->
        val w = size.width
        val h = size.height
        val curveScale = taperFraction / 0.4f
        moveTo(0f, h)
        lineTo(0f, h * taperFraction)
        cubicTo(0f, h * 0.181f * curveScale, w * 0.4f, h * 0.113f * curveScale, w * 0.5f, 0f)
        cubicTo(w * 0.6f, h * 0.113f * curveScale, w, h * 0.181f * curveScale, w, h * taperFraction)
        lineTo(w, h)
        close()
    }

private val MihrabCartoucheShape = mihrabCartoucheShape()
private val MihrabCartoucheShapeShortTaper = mihrabCartoucheShape(0.24f)

private val MihrabFrameTop = Color(0xFFEEC388)
private val MihrabFrameBottom = Color(0xFFB07B3C)
private val MihrabAlertFrameTop = Color(0xFFE9955F)
private val MihrabAlertFrameBottom = Color(0xFF8D4A24)
private val MihrabArchFill = Color(0xFF221913)
private val MihrabBandGold = Color(0xFFC98F4A)
private val MihrabTerracotta = Color(0xFFD97742)

private const val MihrabDigitalAspect = 700f / 470f
private const val MihrabAnalogAspect = 780f / 900f
private const val MihrabBayanAspect = 880f / 520f
private const val MihrabCountdownAspect = 1020f / 640f
private val MihrabCrownOverhead = 36.dp

private fun mihrabArchBodyHeight(available: Dp, fraction: Float): Dp =
    (available * fraction - MihrabCrownOverhead).coerceAtLeast(available * 0.45f)

object MihrabScreenFace : ScreenFace {
    override val id = "mihrab"
    override val name = "Mihrab"
    override val description = "Ornamental arch and lattice prayer band"
    override val recommendedColorThemeId = "sand_terracotta"

    @Composable override fun Dashboard(
        data: ScreenDisplayData,
        callbacks: ScreenFaceCallbacks,
        modifier: Modifier,
    ) = MihrabDashboard(data, callbacks, modifier)

    @Composable override fun Countdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) = MihrabCountdown(data, countdown, modifier)

    @Composable override fun FridayNightBayanCountdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) = MihrabCountdown(data, countdown, modifier)

    @Composable override fun Dua(
        data: ScreenDisplayData,
        modifier: Modifier,
    ) = MihrabDua(data, modifier)

    @Composable override fun FridayBayan(
        data: ScreenDisplayData,
        bayan: JummahData,
        modifier: Modifier,
    ) = MihrabBayan(data, bayan, modifier)
}

@Composable private fun MihrabLattice(modifier: Modifier, band: Boolean = false) {
    val c = LocalColorTheme.current
    Box(modifier.drawWithCache {
        val path = Path()
        if (size.width > 0f && size.height > 0f) {
            val gap = if (band) 18f else (size.minDimension * .065f).coerceAtLeast(1f)
            var x = -size.height
            while (x < size.width + size.height) {
                path.moveTo(x, 0f)
                path.lineTo(x + size.height, size.height)
                path.moveTo(x, size.height)
                path.lineTo(x + size.height, 0f)
                x += gap
            }
        }
        onDrawBehind {
            drawPath(path, c.primaryAccent.copy(alpha = if (band) .10f else .045f), style = Stroke(1f))
        }
    })
}

@Composable private fun MihrabBandLattice(modifier: Modifier) = MihrabLattice(modifier, band = true)

private fun mihrabGregorianLine(h: FaceHeaderData): String =
    "${h.weekday}, ${h.gregorian}"

@Composable
private fun MihrabHeaderDates(
    h: FaceHeaderData,
    c: com.mosque.prayerclock.ui.theme.ColorTheme,
    s: TvTextScale,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.End,
    ) {
        TvAutoSizeText(
            h.hijri,
            c.textSecondary,
            s.height(.048f),
            s.height(.026f),
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        TvAutoSizeText(
            mihrabGregorianLine(h),
            c.textSecondary,
            s.height(.042f),
            s.height(.022f),
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

private data class KhatimDialMetrics(
    val radiusPx: Float,
    val khatimHalfPx: Float,
    /** Radial distance to numeral center — midpoint of the cardinal khatim triangle pocket. */
    val numDistPx: Float,
)

private fun khatimDialMetrics(minDimensionPx: Float): KhatimDialMetrics {
    val half = minDimensionPx * 0.5f
    // Square half-side: kept well inside the dial bounds so the cardinal numerals
    // (which sit beyond the diamond's corners) have room to render without clipping.
    val khatimHalfPx = half * 0.80f / sqrt(2f)
    // Circle nearly fills the upright square, touching close to its edges.
    val radiusPx = khatimHalfPx * 0.92f
    // Midpoint of the cardinal triangle pocket (between the square's edge and the diamond's corner).
    val numDistPx = khatimHalfPx * (1f + sqrt(2f)) * 0.5f
    return KhatimDialMetrics(radiusPx, khatimHalfPx, numDistPx)
}

@Composable private fun MihrabDashboard(
    data: ScreenDisplayData,
    cb: ScreenFaceCallbacks,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(
        modifier.background(c.backgroundMain),
    ) {
        MihrabLattice(Modifier.fillMaxSize())
        val s = rememberTvTextScale(maxWidth, maxHeight)
        // Keep dashboard content inside the same proportional side margin as Ufuq.
        Column(Modifier.fillMaxSize().padding(horizontal = s.widthDp(.025f))) {
            MihrabCompactHeader(
                data,
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
            when (val state = data.mainState) {
                is MainUiState.Success -> {
                    val rows = prayerRows(state.prayerTimes, data.currentTime)
                    val next = resolveDisplayNextPrayer(rows, state.nextPrayer, data.currentTime, state.nextDayFajr)
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable(onClick = cb.onOpenSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        MihrabPrimary(data, next, Modifier.fillMaxSize())
                    }
                    MihrabPrayerBand(
                        data,
                        rows,
                        next.type,
                        Modifier
                            .fillMaxWidth()
                            .height(s.heightDp(.21f)),
                    )
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

@Composable private fun MihrabDiamond(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(size).rotate(45f).background(color))
}

@Composable private fun MihrabOrnamentDivider(modifier: Modifier = Modifier) {
    val c = LocalColorTheme.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.weight(1f).height(2.dp).background(c.uiTertiary.copy(alpha = .35f)))
        MihrabDiamond(10.dp, MihrabBandGold)
        Box(
            Modifier
                .weight(1.6f)
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(c.uiTertiary.copy(alpha = .35f), c.primaryAccent, c.uiTertiary.copy(alpha = .35f)),
                    ),
                ),
        )
        MihrabDiamond(10.dp, MihrabBandGold)
        Box(Modifier.weight(1f).height(2.dp).background(c.uiTertiary.copy(alpha = .35f)))
    }
}

@Composable private fun MihrabCompactHeader(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val h = headerData(data)
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvAutoSizeText(
                    h.mosque.uppercase(),
                    c.primaryAccent,
                    s.height(.065f),
                    s.height(.034f),
                    Modifier.weight(1f, fill = false),
                    fontFamily = AppFonts.Display,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = s.height(.008f),
                    maxLines = 1,
                )
                MihrabHeaderDates(
                    h,
                    c,
                    s,
                    Modifier.weight(1f),
                )
            }
            MihrabOrnamentDivider(Modifier.padding(top = s.heightDp(.012f)))
        }
    }
}

@Composable private fun MihrabSceneHeader(
    data: ScreenDisplayData,
    modifier: Modifier = Modifier,
) {
    val h = headerData(data)
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvAutoSizeText(
                h.mosque.uppercase(),
                c.primaryAccent,
                s.height(.055f),
                s.height(.028f),
                Modifier.weight(1f),
                fontFamily = AppFonts.Display,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = s.height(.008f),
                maxLines = 1,
            )
            MihrabHeaderDates(h, c, s, Modifier.weight(1f))
        }
    }
}

@Composable private fun MihrabMuqarnasCrown(alert: Boolean) {
    val apex = if (alert) MihrabAlertFrameTop else MihrabFrameTop
    val pair = if (alert) MihrabTerracotta else MihrabBandGold
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MihrabDiamond(16.dp, apex)
        Row(
            Modifier.padding(top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MihrabDiamond(11.dp, pair)
            MihrabDiamond(11.dp, pair)
        }
    }
}

@Composable private fun MihrabArchFrame(
    height: Dp,
    aspectRatio: Float,
    alert: Boolean = false,
    showCrown: Boolean = true,
    contentTopFraction: Float = 0.40f,
    centerInArch: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val width = height * aspectRatio
    val frameTop = if (alert) MihrabAlertFrameTop else MihrabFrameTop
    val frameBottom = if (alert) MihrabAlertFrameBottom else MihrabFrameBottom
    val cartoucheShape = if (centerInArch) MihrabCartoucheShapeShortTaper else MihrabCartoucheShape
    Column(
        modifier
            .width(width),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showCrown) {
            MihrabMuqarnasCrown(alert)
            Spacer(Modifier.height(4.dp))
        }
        BoxWithConstraints(
            Modifier
                .width(width)
                .height(height)
                .clipToBounds(),
        ) {
            val contentTop = if (centerInArch) maxHeight * 0.17f else maxHeight * contentTopFraction
            val boxMaxWidth = maxWidth
            val boxMaxHeight = maxHeight
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(frameTop, frameBottom)), cartoucheShape),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(cartoucheShape)
                    .background(MihrabArchFill),
            ) {
                MihrabLattice(Modifier.fillMaxSize())
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(
                            top = contentTop,
                            start = if (centerInArch) boxMaxWidth * 0.01f else 12.dp,
                            end = if (centerInArch) boxMaxWidth * 0.01f else 12.dp,
                            bottom = if (centerInArch) boxMaxHeight * 0.01f else 10.dp,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable private fun MihrabFolioPanel(
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalColorTheme.current
    val cornerDiamond = 16.dp
    val cornerInset = cornerDiamond / 2
    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .border(3.dp, MihrabBandGold, RoundedCornerShape(20.dp))
                .background(MihrabArchFill, RoundedCornerShape(20.dp))
                .padding(horizontal = 48.dp, vertical = 36.dp),
            contentAlignment = Alignment.Center,
            content = content,
        )
        MihrabDiamond(cornerDiamond, c.primaryAccent, Modifier.align(Alignment.TopStart).offset(-cornerInset, -cornerInset))
        MihrabDiamond(cornerDiamond, c.primaryAccent, Modifier.align(Alignment.TopEnd).offset(cornerInset, -cornerInset))
        MihrabDiamond(cornerDiamond, c.primaryAccent, Modifier.align(Alignment.BottomStart).offset(-cornerInset, cornerInset))
        MihrabDiamond(cornerDiamond, c.primaryAccent, Modifier.align(Alignment.BottomEnd).offset(cornerInset, cornerInset))
    }
}

@Composable private fun MihrabNextPrayerPanel(
    data: ScreenDisplayData,
    next: PrayerInfo,
    s: TvTextScale,
    modifier: Modifier = Modifier,
) {
    val c = LocalColorTheme.current
    val event = inlineCountdown(data, next)
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TvAutoSizeText(
            localizedStringResource(R.string.next_prayer),
            c.textSecondary,
            s.height(.055f),
            s.height(.028f),
            letterSpacing = s.height(.014f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        TvAutoSizeText(
            prayerWithEventLabel(next, event.isIqamah),
            c.primaryAccent,
            s.height(.14f),
            s.height(.07f),
            Modifier.padding(top = s.heightDp(.006f)),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        TvAutoSizeText(
            TimeUtils.formatTimeBasedOnPreference(event.targetTime, data.settings.show24HourFormat),
            c.textPrimary,
            s.height(.13f),
            s.height(.065f),
            Modifier.padding(top = s.heightDp(.004f)),
            fontFamily = AppFonts.Display,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        next.iqamahTime?.let {
            TvAutoSizeText(
                "${localizedStringResource(R.string.iqamah)} ${
                    TimeUtils.formatTimeBasedOnPreference(it, data.settings.show24HourFormat)
                }",
                c.textSecondary,
                s.height(.058f),
                s.height(.03f),
                Modifier.padding(top = s.heightDp(.012f)),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable private fun MihrabAnalogSidePanel(
    data: ScreenDisplayData,
    next: PrayerInfo,
    s: TvTextScale,
    modifier: Modifier = Modifier,
) {
    val c = LocalColorTheme.current
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TvAutoSizeText(
            data.clock.formattedTime,
            c.textPrimary,
            s.height(.15f),
            s.height(.08f),
            fontFamily = AppFonts.Display,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Row(
            Modifier.padding(vertical = s.heightDp(.014f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.width(120.dp).height(2.dp).background(c.uiTertiary.copy(alpha = .35f)))
            Box(Modifier.width(120.dp).height(2.dp).background(c.uiTertiary.copy(alpha = .35f)))
        }
        MihrabNextPrayerPanel(data, next, s)
    }
}

@Composable private fun MihrabPrimary(
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
        val archAspect = if (analog) MihrabAnalogAspect else MihrabDigitalAspect
        val archHeight =
            minOf(
                mihrabArchBodyHeight(maxHeight, 0.98f),
                maxWidth * (if (analog) 0.46f else 0.42f) / archAspect,
            )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.Start),
        ) {
            MihrabArchFrame(
                height = archHeight,
                aspectRatio = archAspect,
                centerInArch = analog,
                contentTopFraction = .32f,
            ) {
                if (analog) {
                    BoxWithConstraints(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val dialSize = minOf(maxWidth * 0.99f, maxHeight * 0.99f)
                        MihrabKhatimDial(data, Modifier.size(dialSize))
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TvAutoSizeText(
                            localizedStringResource(R.string.now_label),
                            c.textSecondary,
                            s.height(.048f),
                            s.height(.024f),
                            letterSpacing = s.height(.014f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                        FacePanelClock(
                            data.clock, c.textPrimary, c.textSecondary,
                            Modifier.weight(1f).fillMaxWidth(),
                            fontFamily = AppFonts.Display,
                            fontWeight = FontWeight.SemiBold,
                            secondaryScale = .36f,
                            horizontalArrangement = Arrangement.Center,
                            amLabel = localizedStringResource(R.string.am),
                            pmLabel = localizedStringResource(R.string.pm),
                        )
                        Row(
                            Modifier.padding(top = s.heightDp(.01f)),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            MihrabDiamond(14.dp, c.primaryAccent)
                            MihrabDiamond(14.dp, MihrabBandGold)
                            MihrabDiamond(14.dp, c.primaryAccent)
                        }
                    }
                }
            }
            if (data.isCountdownVisibleForWeight) {
                MihrabCountdownPanel(data, next, s, Modifier.weight(1f).height(archHeight))
            } else if (analog) {
                MihrabAnalogSidePanel(
                    data,
                    next,
                    s,
                    Modifier.weight(1f),
                )
            } else {
                MihrabNextPrayerPanel(
                    data,
                    next,
                    s,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable private fun MihrabKhatimDial(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val density = LocalDensity.current
    val hourAngle by
        rememberClockwiseHandAngle(
            clockHourAngle(data.clock.hour12, data.clock.minute, data.clock.second),
            "mihrab_hour",
        )
    val minuteAngle by
        rememberClockwiseHandAngle(
            clockMinuteAngle(data.clock.minute, data.clock.second),
            "mihrab_minute",
        )
    val secondAngle by rememberClockwiseHandAngle(clockSecondAngle(data.clock.second), "mihrab_second")
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val minPx = with(density) { minOf(maxWidth, maxHeight).toPx() }
        val metrics = khatimDialMetrics(minPx)
        val numDistDp = with(density) { metrics.numDistPx.toDp() }
        val numSize = with(density) { (minPx * 0.11f).toSp() }
        val ringWidth = (minPx * 0.012f).coerceIn(2f, 4f)
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val r = metrics.radiusPx
            val khatimHalf = metrics.khatimHalfPx
            val khatimSize = khatimHalf * 2f
            val khatimTopLeft = Offset(center.x - khatimHalf, center.y - khatimHalf)
            val khatimStroke = Stroke(2f)
            drawRect(
                c.primaryAccent.copy(alpha = 0.32f),
                topLeft = khatimTopLeft,
                size = androidx.compose.ui.geometry.Size(khatimSize, khatimSize),
                style = khatimStroke,
            )
            withTransform({
                rotate(45f, center)
            }) {
                drawRect(
                    c.primaryAccent.copy(alpha = 0.32f),
                    topLeft = khatimTopLeft,
                    size = androidx.compose.ui.geometry.Size(khatimSize, khatimSize),
                    style = khatimStroke,
                )
            }
            drawCircle(c.primaryAccent, r, center, style = Stroke(ringWidth))
            drawCircle(MihrabArchFill, (r - ringWidth * 0.45f).coerceAtLeast(0f), center)
            drawMinuteTickRing(center, r * .94f, r * .87f, c.primaryAccent)
            drawRegulatorMinuteMarks(center, r * 1.06f, c.primaryAccent)
            drawClockHand(center, r * 0.60f, hourAngle, c.textPrimary, r * 0.034f)
            drawClockHand(center, r * 0.84f, minuteAngle, c.textPrimary, r * 0.018f)
            if (data.clock.showSeconds) {
                drawClockHand(center, r * 0.89f, secondAngle, MihrabTerracotta, r * 0.008f)
            }
            drawCircle(c.primaryAccent, r * 0.068f, center)
        }
        val numStyle = AppFonts.Arabic
        Box(Modifier.fillMaxSize()) {
            Text(
                "١٢",
                color = c.primaryAccent,
                fontFamily = numStyle,
                fontSize = numSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).offset(y = -numDistDp),
            )
            Text(
                "٣",
                color = c.primaryAccent,
                fontFamily = numStyle,
                fontSize = numSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).offset(x = numDistDp),
            )
            Text(
                "٦",
                color = c.primaryAccent,
                fontFamily = numStyle,
                fontSize = numSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).offset(y = numDistDp),
            )
            Text(
                "٩",
                color = c.primaryAccent,
                fontFamily = numStyle,
                fontSize = numSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).offset(x = -numDistDp),
            )
        }
    }
}

@Composable private fun MihrabPrayerBand(
    data: ScreenDisplayData,
    rows: List<PrayerInfo>,
    next: PrayerType?,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(MihrabBandGold))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MihrabArchFill),
        ) {
            MihrabBandLattice(Modifier.fillMaxSize())
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp, horizontal = 2.dp),
            ) {
                val s = rememberTvTextScale(maxWidth, maxHeight)
                val nameSize = s.height(.23f)
                val timeSize = s.fit(
                    heightFraction = 0.38f,
                    characterSlots = 8f,
                    glyphWidthFactor = 1.15f,
                )
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    rows.forEachIndexed { index, prayer ->
                        val isNext = prayer.type == next
                        val isSunrise = prayer.type == PrayerType.SUNRISE
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = s.widthDp(.005f))
                                .then(
                                    if (isNext) {
                                        Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(c.primaryAccent.copy(alpha = 0.13f))
                                            .padding(vertical = 4.dp)
                                    } else {
                                        Modifier
                                    },
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            TvAutoSizeText(
                                prayer.name,
                                when {
                                    isNext -> c.primaryAccent
                                    isSunrise -> MihrabTerracotta
                                    else -> c.textSecondary
                                },
                                nameSize,
                                nameSize * 0.55f,
                                fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                            TvAutoSizeText(
                                TimeUtils.formatTimeBasedOnPreference(prayer.azanTime, data.settings.show24HourFormat),
                                when {
                                    isNext -> c.primaryAccent
                                    isSunrise -> MihrabTerracotta
                                    else -> c.textPrimary
                                },
                                timeSize,
                                timeSize * 0.58f,
                                Modifier.padding(top = s.heightDp(.004f)),
                                fontFamily = AppFonts.Display,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                        if (index < rows.lastIndex) {
                            MihrabDiamond(10.dp, MihrabBandGold)
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(MihrabBandGold))
    }
}

@Composable private fun MihrabCountdownPanel(
    data: ScreenDisplayData,
    p: PrayerInfo,
    s: TvTextScale,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val cd = inlineCountdown(data, p)
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TvAutoSizeText(
            prayerCountdownHeading(p, cd.isIqamah),
            if (cd.isIqamah) c.uiTertiary else MihrabAlertFrameTop,
            s.height(.058f),
            s.height(.03f),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = s.height(.012f),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            TvAutoSizeText(
                "%02d:%02d".format(cd.minutes, cd.seconds),
                c.textPrimary,
                s.height(.50f),
                s.height(.13f),
                Modifier.padding(top = s.heightDp(.004f)),
                fontFamily = AppFonts.Display,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        MihrabDiamond(
            16.dp,
            MihrabTerracotta,
            Modifier.padding(vertical = s.heightDp(.012f)),
        )
        if (p.type != PrayerType.SUNRISE) {
            TvAutoSizeText(
                "${localizedStringResource(R.string.azan)} ${
                    TimeUtils.formatTimeBasedOnPreference(p.azanTime, data.settings.show24HourFormat)
                }",
                c.primaryAccent,
                s.height(.068f),
                s.height(.034f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            p.iqamahTime?.let {
                TvAutoSizeText(
                    "${localizedStringResource(R.string.iqamah)} ${
                        TimeUtils.formatTimeBasedOnPreference(it, data.settings.show24HourFormat)
                    }",
                    c.textSecondary,
                    s.height(.058f),
                    s.height(.03f),
                    Modifier.padding(top = s.heightDp(.004f)),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable private fun MihrabCountdown(
    data: ScreenDisplayData,
    v: FullScreenCountdownData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(
        modifier
            .background(c.backgroundMain)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        MihrabLattice(Modifier.fillMaxSize())
        val s = rememberTvTextScale(maxWidth, maxHeight)
        val archHeight = mihrabArchBodyHeight(maxHeight, 0.82f)
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            MihrabSceneHeader(data, Modifier.fillMaxWidth())
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f),
            ) {
                MihrabArchFrame(
                    height = archHeight,
                    aspectRatio = MihrabCountdownAspect,
                    centerInArch = true,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        TvAutoSizeText(
                            fullScreenCountdownTitle(v.prayerName, v.prayerType, v.isIqamah, v.kind),
                            c.primaryAccent,
                            s.height(.072f),
                            s.height(.036f),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                        TvAutoSizeText(
                            "%02d:%02d".format(v.minutes, v.seconds),
                            c.textPrimary,
                            s.height(.26f),
                            s.height(.14f),
                            Modifier.padding(top = s.heightDp(.008f)),
                            fontFamily = AppFonts.Display,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                        Row(
                            Modifier.fillMaxWidth(.75f).padding(top = s.heightDp(.006f)),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.widthDp(.014f)),
                        ) {
                            MihrabDiamond(13.dp, MihrabTerracotta)
                            TvAutoSizeText(
                                TimeUtils.formatTimeBasedOnPreference(v.targetTime, data.settings.show24HourFormat),
                                c.textSecondary,
                                s.height(.085f),
                                s.height(.044f),
                                modifier = Modifier.weight(1f),
                                fontFamily = AppFonts.Display,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                            MihrabDiamond(13.dp, MihrabTerracotta)
                        }
                    }
                }
            }
            Spacer(Modifier.height(s.heightDp(.02f)))
        }
    }
}

@Composable private fun MihrabBayan(
    data: ScreenDisplayData,
    v: JummahData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(
        modifier
            .background(c.backgroundMain)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        MihrabLattice(Modifier.fillMaxSize())
        val s = rememberTvTextScale(maxWidth, maxHeight)
        val archHeight = mihrabArchBodyHeight(maxHeight, 0.52f)
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            MihrabSceneHeader(data, Modifier.fillMaxWidth())
            TvAutoSizeText(
                localizedStringResource(R.string.silent_your_phone).uppercase(),
                MihrabTerracotta,
                s.height(.048f),
                s.height(.024f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = s.height(.012f),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            MihrabArchFrame(
                height = archHeight,
                aspectRatio = MihrabBayanAspect,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TvAutoSizeText(
                        localizedStringResource(R.string.jummah_in_progress_title),
                        c.primaryAccent,
                        s.height(.068f),
                        s.height(.034f),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                    TvAutoSizeText(
                        v.formattedRemaining(),
                        c.textPrimary,
                        s.height(.18f),
                        s.height(.10f),
                        Modifier.padding(top = s.heightDp(.002f)),
                        fontFamily = AppFonts.Display,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
            TvAutoSizeText(
                localizedStringResource(R.string.jummah_prayer_reminder),
                c.textSecondary.copy(alpha = 0.85f),
                s.height(.058f),
                s.height(.03f),
                Modifier.fillMaxWidth(.92f),
                textAlign = TextAlign.Center,
                maxLines = 4,
            )
        }
    }
}

@Composable private fun MihrabDua(
    @Suppress("UNUSED_PARAMETER") data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(
        modifier
            .background(c.backgroundMain)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        MihrabLattice(Modifier.fillMaxSize())
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvAutoSizeText(
                    localizedStringResource(R.string.silent_your_phone),
                    MihrabTerracotta,
                    s.height(.048f),
                    s.height(.024f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = s.height(.01f),
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                TvAutoSizeText(
                    localizedStringResource(R.string.dua_joining_saff_title),
                    c.primaryAccent,
                    s.height(.048f),
                    s.height(.024f),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
            }
            MihrabFolioPanel(
                Modifier
                    .fillMaxWidth(.92f)
                    .weight(1f)
                    .padding(vertical = s.heightDp(.02f)),
            ) {
                TvAutoSizeText(
                    localizedStringResource(R.string.dua_joining_saff_arabic),
                    c.textPrimary,
                    s.height(.11f),
                    s.height(.055f),
                    fontFamily = AppFonts.Arabic,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TvAutoSizeText(
                    localizedStringResource(R.string.dua_joining_saff_transliteration),
                    c.textPrimary,
                    s.height(.068f),
                    s.height(.034f),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                TvAutoSizeText(
                    localizedStringResource(R.string.dua_joining_saff_meaning),
                    c.textSecondary,
                    s.height(.052f),
                    s.height(.026f),
                    Modifier
                        .fillMaxWidth(.88f)
                        .padding(top = s.heightDp(.01f)),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                )
            }
        }
    }
}
