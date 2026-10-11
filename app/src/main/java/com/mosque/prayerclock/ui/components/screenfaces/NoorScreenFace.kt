package com.mosque.prayerclock.ui.components.screenfaces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.components.*
import com.mosque.prayerclock.ui.components.screenfaces.shared.*
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.LocalColorTheme
import com.mosque.prayerclock.utils.TimeUtils
import com.mosque.prayerclock.viewmodel.MainUiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object NoorScreenFace : ScreenFace {
    override val id = "noor"
    override val name = "Noor"
    override val description = "High-contrast matrix mosque board"
    override val recommendedColorThemeId = "amber_matrix"

    @Composable override fun Dashboard(
        data: ScreenDisplayData,
        callbacks: ScreenFaceCallbacks,
        modifier: Modifier,
    ) = NoorDashboard(data, callbacks, modifier)

    @Composable override fun Countdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) = NoorScene(
        modifier,
        fullScreenCountdownTitle(
            countdown.prayerName,
            countdown.prayerType,
            countdown.isIqamah,
            countdown.kind,
        ),
        "%02d:%02d".format(countdown.minutes, countdown.seconds),
        TimeUtils.formatTimeBasedOnPreference(countdown.targetTime, data.settings.show24HourFormat),
    )

    @Composable override fun FridayNightBayanCountdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) = Countdown(data, countdown, modifier)

    @Composable override fun Dua(
        data: ScreenDisplayData,
        modifier: Modifier,
    ) = NoorDua(modifier)

    @Composable override fun FridayBayan(
        data: ScreenDisplayData,
        bayan: JummahData,
        modifier: Modifier,
    ) = NoorScene(
        modifier,
        localizedStringResource(R.string.jummah_in_progress_title),
        bayan.formattedRemaining(),
        localizedStringResource(R.string.jummah_prayer_reminder),
    )
}

@Composable private fun NoorGround(modifier: Modifier) {
    val c = LocalColorTheme.current
    Canvas(modifier) {
        val pitch = 16.dp.toPx()
        var y =
            pitch / 2
        while (y <
            size.height
        ) {
            var x =
                pitch / 2
            while (x <
                size.width
            ) {
                drawCircle(c.azanTime.copy(alpha = .09f), 1.5.dp.toPx(), Offset(x, y))
                x += pitch
            }
            y += pitch
        }
    }
}

@Composable private fun NoorDashboard(
    data: ScreenDisplayData,
    cb: ScreenFaceCallbacks,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain)) {
        NoorGround(Modifier.fillMaxSize())
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = s.heightDp(.02f))) {
            NoorHeader(data, Modifier.fillMaxWidth().height(s.heightDp(.125f)))
            when (val state = data.mainState) {
                is MainUiState.Success -> {
                    val rows = prayerRows(state.prayerTimes, data.currentTime)
                    val next = resolveDisplayNextPrayer(rows, state.nextPrayer, data.currentTime, state.nextDayFajr)
                    if (data.isCountdownVisibleForWeight) {
                        NoorAzanHour(
                            data,
                            next,
                            Modifier.weight(.60f).fillMaxWidth().clickable(onClick = cb.onOpenSettings),
                        )
                        NoorSixColumnRail(
                            data,
                            rows,
                            next.type,
                            Modifier.weight(.32f).fillMaxWidth(),
                        )
                    } else {
                        NoorPrimary(
                            data,
                            next,
                            Modifier.weight(.46f).fillMaxWidth().clickable(onClick = cb.onOpenSettings),
                        )
                        NoorTable(data, rows, next.type, Modifier.weight(.54f).fillMaxWidth())
                    }
                }

                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TvAutoSizeText(
                            data.errorMessage ?: "…",
                            c.azanTime,
                            s.height(.08f),
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun NoorHeader(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val h = headerData(data)
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvAutoSizeText(
                    h.mosque.uppercase(),
                    c.azanTime,
                    s.height(.62f),
                    s.height(.34f),
                    Modifier.weight(1f),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = s.height(.012f),
                    lineHeightRatio = 1.08f,
                    maxLines = 1,
                )
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                ) {
                    TvAutoSizeText(
                        h.hijri,
                        c.azanTime,
                        s.height(.46f),
                        s.height(.26f),
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        lineHeightRatio = 1.08f,
                        maxLines = 1,
                    )
                    TvAutoSizeText(
                        "${h.weekday} · ${h.gregorian}",
                        c.azanTime,
                        s.height(.46f),
                        s.height(.26f),
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        lineHeightRatio = 1.08f,
                        maxLines = 1,
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(c.azanTime.copy(alpha = .32f)))
        }
    }
}

@Composable private fun NoorPrimary(
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
            Modifier.fillMaxSize().padding(vertical = s.heightDp(.025f)),
            horizontalArrangement = Arrangement.spacedBy(s.widthDp(.018f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(.54f)
                    .fillMaxHeight()
                    .background(c.surfaceCard.copy(alpha = .75f))
                    .border(1.dp, c.azanTime.copy(alpha = .22f))
                    .padding(horizontal = s.widthDp(.01f)),
                contentAlignment = Alignment.Center,
            ) {
                if (analog) {
                    NoorDial(
                        data,
                        Modifier
                            .fillMaxHeight(.96f)
                            .fillMaxWidth(.98f),
                    )
                } else {
                    NoorMatrixClock(
                        clock = data.clock,
                        color = c.azanTime,
                        maxHeightFraction = .95f,
                        modifier = Modifier.fillMaxHeight().fillMaxWidth(.98f),
                        shadow = Shadow(c.azanTime.copy(alpha = .5f), blurRadius = 18f),
                        amLabel = localizedStringResource(R.string.am),
                        pmLabel = localizedStringResource(R.string.pm),
                    )
                }
            }
            Box(
                Modifier
                    .weight(.46f)
                    .fillMaxHeight()
                    .background(c.surfaceCard.copy(alpha = .75f))
                    .border(1.dp, c.azanTime.copy(alpha = .22f))
                    .padding(horizontal = s.widthDp(.025f)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(s.heightDp(.012f)),
                ) {
                    TvAutoSizeText(
                        localizedStringResource(R.string.next_prayer).uppercase(),
                        c.textSecondary,
                        s.height(.08f),
                        s.height(.04f),
                        fontFamily = FontFamily.Monospace,
                    )
                    TvAutoSizeText(
                        next.name,
                        c.azanTime,
                        s.height(.25f),
                        s.height(.11f),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                    TvAutoSizeText(
                        TimeUtils.formatTimeBasedOnPreference(next.azanTime, data.settings.show24HourFormat),
                        c.primaryAccent,
                        s.height(.23f),
                        s.height(.10f),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                    next.iqamahTime?.let { time ->
                        TvAutoSizeText(
                            "${localizedStringResource(R.string.iqamah_letter)} " +
                                TimeUtils.formatTimeBasedOnPreference(time, data.settings.show24HourFormat),
                            c.textSecondary, s.height(.085f), s.height(.04f),
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun NoorDial(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val clock = data.clock
    val secondProgress = clock.second / 60f
    val minuteProgress = (clock.minute + clock.second / 60f) / 60f
    val hourProgress =
        if (clock.show24Hour) {
            (clock.hour24 + clock.minute / 60f + clock.second / 3600f) / 24f
        } else {
            (clock.hour12 % 12 + clock.minute / 60f + clock.second / 3600f) / 12f
        }
    val hourText =
        if (clock.show24Hour) {
            "%02d:%02d".format(clock.hour24, clock.minute)
        } else {
            "%d:%02d".format(if (clock.hour12 == 0) 12 else clock.hour12, clock.minute)
        }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension * 0.48f
            val dimTicks = c.azanTime.copy(alpha = 0.20f)
            val gapColor = c.backgroundMain

            drawCircle(
                c.azanTime.copy(alpha = 0.35f),
                r * 0.99f,
                center,
                style = Stroke(width = r * 0.005f),
            )
            // Outer — seconds: long radial ticks + accent arc + marker
            drawNoorProgressRing(
                center,
                r * 0.975f,
                r * 0.895f,
                if (clock.showSeconds) secondProgress else 0f,
                c.primaryAccent,
                dimTicks,
                NoorRingKind.Seconds,
            )
            if (clock.showSeconds) {
                drawNoorSecondMarker(center, r * 0.935f, clock.second, c.primaryAccent, r * 0.026f)
            }
            drawNoorRingSeparator(center, r * 0.888f, gapColor, r * 0.014f)
            // Middle — minutes: major 12-tick ring + round-cap arc
            drawNoorProgressRing(
                center,
                r * 0.875f,
                r * 0.795f,
                minuteProgress,
                c.azanTime,
                dimTicks.copy(alpha = 0.28f),
                NoorRingKind.Minutes,
            )
            drawNoorRingSeparator(center, r * 0.788f, gapColor, r * 0.012f)
            // Inner — hours: square hour blocks + thin arc
            drawNoorProgressRing(
                center,
                r * 0.775f,
                r * 0.705f,
                hourProgress,
                c.primaryAccent.copy(alpha = 0.82f),
                dimTicks.copy(alpha = 0.35f),
                NoorRingKind.Hours,
            )
            drawCircle(c.surfaceCard, r * 0.48f, center)
            drawCircle(
                c.azanTime.copy(alpha = 0.28f),
                r * 0.48f,
                center,
                style = Stroke(width = r * 0.004f),
            )
        }
        Column(
            Modifier.fillMaxWidth(0.38f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TvAutoSizeText(
                hourText,
                c.azanTime,
                s.height(.20f),
                s.height(.10f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeightRatio = 1.02f,
                maxLines = 1,
                shadow = Shadow(c.azanTime.copy(alpha = 0.5f), blurRadius = 16f),
            )
            if (!clock.show24Hour) {
                TvAutoSizeText(
                    localizedStringResource(if (clock.isAm) R.string.am else R.string.pm),
                    c.primaryAccent, s.height(.065f), s.height(.035f),
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable private fun NoorTable(
    data: ScreenDisplayData,
    rows: List<PrayerInfo>,
    next: PrayerType?,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val scheduleRows =
        rows.filter {
            it.type != PrayerType.SUNRISE
        }
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        // Reserve a compact heading, then share the remaining height equally between prayers.
        // Each cell fits its text to that row rather than to the height of the entire table.
        val columnWeights = listOf(.36f, .32f, .32f)
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(s.heightDp(.13f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(
                    localizedStringResource(R.string.prayer),
                    localizedStringResource(R.string.azan),
                    localizedStringResource(R.string.iqamah_letter),
                ).forEachIndexed { index, label ->
                    NoorTableCell(
                        label.uppercase(),
                        c.textSecondary,
                        Modifier.weight(columnWeights[index]).fillMaxHeight(),
                        fontWeight = FontWeight.Normal,
                        heightFraction = .72f,
                    )
                }
            }
            Box(
                Modifier.fillMaxWidth().height(s.heightDp(.004f))
                    .background(c.azanTime.copy(alpha = .32f)),
            )
            scheduleRows.forEach { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            if (p.type == next) {
                                c.azanTime.copy(alpha = .14f)
                            } else {
                                Color.Transparent
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NoorTableCell(
                        p.name,
                        if (p.type == next) c.primaryAccent else c.azanTime,
                        Modifier.weight(columnWeights[0]).fillMaxHeight(),
                    )
                    NoorTableCell(
                        TimeUtils.formatTimeWithoutPeriod(p.azanTime, data.settings.show24HourFormat),
                        c.azanTime,
                        Modifier.weight(columnWeights[1]).fillMaxHeight(),
                    )
                    NoorTableCell(
                        p.iqamahTime
                            ?.let {
                                TimeUtils.formatTimeWithoutPeriod(it, data.settings.show24HourFormat)
                            }.orEmpty(),
                        c.textSecondary,
                        Modifier.weight(columnWeights[2]).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable private fun NoorTableCell(
    text: String,
    color: Color,
    modifier: Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
    heightFraction: Float = .82f,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        TvAutoSizeText(
            text,
            color,
            s.height(heightFraction),
            s.height(.40f),
            Modifier.fillMaxWidth(.94f),
            fontFamily = FontFamily.Monospace,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            lineHeightRatio = 1.08f,
            maxLines = 1,
        )
    }
}

@Composable private fun NoorAzanHour(
    data: ScreenDisplayData,
    next: PrayerInfo,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val cd = inlineCountdown(data, next)
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(
            Modifier
                .fillMaxSize()
                .padding(vertical = s.heightDp(.012f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            TvAutoSizeText(
                prayerCountdownHeading(next, cd.isIqamah),
                c.primaryAccent,
                s.height(.13f),
                s.height(.07f),
                Modifier.fillMaxWidth(.94f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            val digitSize = minOf(s.heightDp(.58f), s.widthDp(.20f))
            val colonSize = s.height(.24f)
            Row(
                horizontalArrangement = Arrangement.spacedBy(s.widthDp(.014f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NoorCountdownDigit(
                    "%02d".format(cd.minutes),
                    digitSize,
                    s.height(.48f),
                    s.height(.19f),
                )
                Text(
                    ":",
                    color = c.azanTime.copy(alpha = 0.45f),
                    fontSize = colonSize.value.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(s.widthDp(.028f)),
                    textAlign = TextAlign.Center,
                )
                NoorCountdownDigit(
                    "%02d".format(cd.seconds),
                    digitSize,
                    s.height(.48f),
                    s.height(.19f),
                )
            }
            PrayerStatusLine(
                data,
                next,
                c.textSecondary,
                s.height(.17f),
                s.height(.085f),
                Modifier
                    .fillMaxWidth(.96f)
                    .height(s.heightDp(.15f)),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable private fun NoorCountdownDigit(
    text: String,
    boxSize: androidx.compose.ui.unit.Dp,
    maxFontSize: androidx.compose.ui.unit.TextUnit,
    minFontSize: androidx.compose.ui.unit.TextUnit,
) {
    val c = LocalColorTheme.current
    Box(
        Modifier
            .size(boxSize)
            .border(3.dp, c.azanTime.copy(alpha = 0.45f))
            .background(c.surfaceCard),
        contentAlignment = Alignment.Center,
    ) {
        TvAutoSizeText(
            text,
            c.azanTime,
            maxFontSize,
            minFontSize,
            Modifier.fillMaxWidth(.88f),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            shadow = Shadow(c.azanTime.copy(alpha = 0.5f), blurRadius = 22f),
        )
    }
}

@Composable private fun NoorSixColumnRail(
    data: ScreenDisplayData,
    rows: List<PrayerInfo>,
    next: PrayerType?,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(c.azanTime.copy(alpha = 0.32f)),
            )
            Row(Modifier.fillMaxSize().padding(horizontal = s.widthDp(.006f))) {
                rows.forEach { p ->
                    val isNext = p.type == next
                    val isSunrise = p.type == PrayerType.SUNRISE
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (isNext) c.azanTime.copy(alpha = 0.14f) else Color.Transparent,
                            )
                            .padding(horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        TvAutoSizeText(
                            p.name,
                            when {
                                isNext -> c.primaryAccent
                                isSunrise -> c.uiTertiary
                                else -> c.textSecondary
                            },
                            s.height(.20f),
                            s.height(.11f),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            lineHeightRatio = 1.05f,
                            maxLines = 2,
                        )
                        TvAutoSizeText(
                            TimeUtils.formatTimeWithoutPeriod(p.azanTime, data.settings.show24HourFormat),
                            when {
                                isNext -> c.primaryAccent
                                isSunrise -> c.uiTertiary
                                else -> c.azanTime
                            },
                            s.height(.32f),
                            s.height(.17f),
                            Modifier.padding(top = s.heightDp(.008f)),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            shadow = if (isNext) Shadow(c.azanTime.copy(alpha = 0.35f), blurRadius = 12f) else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun NoorScene(
    modifier: Modifier,
    title: String,
    hero: String,
    footer: String,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain), contentAlignment = Alignment.Center) {
        NoorGround(Modifier.fillMaxSize())
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxWidth(.82f), horizontalAlignment = Alignment.CenterHorizontally) {
            TvAutoSizeText(
                title,
                c.primaryAccent,
                s.height(.09f),
                s.height(.045f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            TvAutoSizeText(
                hero,
                c.azanTime,
                s.height(.36f),
                s.height(.19f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                shadow = Shadow(c.azanTime.copy(alpha = .5f), blurRadius = 22f),
            )
            Box(Modifier.fillMaxWidth(.5f).height(3.dp).background(c.azanTime))
            TvAutoSizeText(
                footer,
                c.textSecondary,
                s.height(.12f),
                s.height(.065f),
                Modifier.padding(top = 12.dp),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
    }
}

@Composable private fun NoorDua(modifier: Modifier) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain), contentAlignment = Alignment.Center) {
        NoorGround(Modifier.fillMaxSize())
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(
            Modifier.fillMaxWidth(.88f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(s.heightDp(.025f)),
        ) {
            TvAutoSizeText(
                localizedStringResource(R.string.silent_your_phone).uppercase(),
                c.textSecondary,
                s.height(.05f),
                s.height(.025f),
                fontFamily = FontFamily.Monospace,
            )
            Box(Modifier.fillMaxWidth().height(2.dp).background(c.azanTime))
            TvAutoSizeText(
                localizedStringResource(R.string.dua_joining_saff_arabic),
                c.azanTime,
                s.height(.14f),
                s.height(.07f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            TvAutoSizeText(
                localizedStringResource(R.string.dua_joining_saff_transliteration),
                c.primaryAccent,
                s.height(.065f),
                s.height(.032f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            TvAutoSizeText(
                localizedStringResource(R.string.dua_joining_saff_meaning),
                c.textSecondary,
                s.height(.05f),
                s.height(.025f),
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
    }
}
