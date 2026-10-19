package com.mosque.prayerclock.ui.components.screenfaces

import com.mosque.prayerclock.ui.components.screenfaces.shared.PrayerDataUnavailable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.components.TvAutoSizeText
import com.mosque.prayerclock.ui.components.getCountdownData
import com.mosque.prayerclock.ui.components.rememberTvTextScale
import com.mosque.prayerclock.ui.components.screenfaces.shared.*
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.LocalColorTheme
import com.mosque.prayerclock.utils.TimeUtils
import com.mosque.prayerclock.viewmodel.MainUiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object SafaaScreenFace : ScreenFace {
    override val id = "safaa"
    override val name = "Safaa"
    override val description = "Calm ruled page with a Roman regulator dial"
    override val recommendedColorThemeId = "notice_paper"

    @Composable override fun Dashboard(
        data: ScreenDisplayData,
        callbacks: ScreenFaceCallbacks,
        modifier: Modifier,
    ) = SafaaDashboard(data, callbacks, modifier)

    @Composable override fun Countdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) = SafaaCountdown(data, countdown, modifier)

    @Composable override fun FridayNightBayanCountdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) = SafaaCountdown(data, countdown, modifier)

    @Composable override fun Dua(
        data: ScreenDisplayData,
        modifier: Modifier,
    ) = SafaaDua(modifier)

    @Composable override fun FridayBayan(
        data: ScreenDisplayData,
        bayan: JummahData,
        modifier: Modifier,
    ) = SafaaBayan(bayan, modifier)
}

@Composable private fun SafaaDashboard(
    data: ScreenDisplayData,
    callbacks: ScreenFaceCallbacks,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain).padding(horizontal = 12.dp, vertical = 10.dp)) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize()) {
            SafaaHeader(data, Modifier.fillMaxWidth().height(s.heightDp(.20f)))
            when (val state = data.mainState) {
                is MainUiState.Success -> {
                    val rows = prayerRows(state.prayerTimes, data.currentTime)
                    val next = resolveDisplayNextPrayer(rows, state.nextPrayer, data.currentTime, state.nextDayFajr)
                    if (data.isCountdownVisibleForWeight) {
                        SafaaAzanHour(
                            data,
                            next,
                            Modifier.weight(.58f).fillMaxWidth().clickable(onClick = callbacks.onOpenSettings),
                        )
                        SafaaSixColumnRail(
                            data,
                            rows,
                            next.type,
                            Modifier.weight(.30f).fillMaxWidth(),
                        )
                    } else {
                        SafaaPrimary(
                            data,
                            next,
                            Modifier.weight(.54f).fillMaxWidth().clickable(onClick = callbacks.onOpenSettings),
                        )
                        SafaaTable(data, rows, next.type, Modifier.weight(.28f).fillMaxWidth())
                    }
                }

                else -> PrayerDataUnavailable(data, callbacks, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable private fun SafaaHeader(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val h = headerData(data)
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TvAutoSizeText(
                    localizedStringResource(R.string.safaa_kicker).uppercase(),
                    c.primaryAccent,
                    s.height(.14f),
                    s.height(.075f),
                    Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = s.height(.010f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            TvAutoSizeText(
                h.mosque,
                c.textPrimary,
                s.height(.28f),
                s.height(.15f),
                Modifier.fillMaxWidth().padding(top = 2.dp),
                FontFamily.Serif,
                FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Box(
                Modifier.fillMaxWidth().padding(top = 6.dp).height(3.dp).background(c.textPrimary.copy(alpha = .85f)),
            )
            Row(
                Modifier.fillMaxWidth().height(s.heightDp(.46f)).padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val metaSize = s.height(.24f)
                val metaMin = s.height(.12f)
                TvAutoSizeText(
                    h.hijri,
                    c.textPrimary,
                    metaSize,
                    metaMin,
                    Modifier.weight(1f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                TvAutoSizeText(
                    "${h.weekday}, ${h.gregorian}",
                    c.textPrimary,
                    metaSize,
                    metaMin,
                    Modifier.weight(1.2f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                if (h.weatherAvailable) {
                    Row(Modifier.weight(.7f), verticalAlignment = Alignment.CenterVertically) {
                        FaceWeatherIcon(
                            h,
                            c.textPrimary,
                            s.heightDp(.34f),
                            Modifier.padding(end = s.widthDp(.010f)),
                        )
                        TvAutoSizeText(
                            h.temperature,
                            c.textPrimary,
                            metaSize,
                            metaMin,
                            Modifier.weight(1f),
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.textSecondary.copy(alpha = .65f)))
        }
    }
}

@Composable private fun SafaaPrimary(
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
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1.15f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                if (analog) {
                    SafaaDial(data, Modifier.fillMaxHeight(.92f).fillMaxWidth(.78f))
                } else {
                    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                        TvAutoSizeText(
                            localizedStringResource(R.string.current_time).uppercase(),
                            c.uiTertiary,
                            s.height(.055f),
                            s.height(.028f),
                            fontFamily = FontFamily.Serif,
                            letterSpacing = s.height(.008f),
                        )
                        FacePanelClock(
                            clock = data.clock,
                            primaryColor = c.textPrimary,
                            mutedColor = c.uiTertiary,
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = s.heightDp(.012f)),
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            secondaryScale = .38f,
                            amLabel = localizedStringResource(R.string.am),
                            pmLabel = localizedStringResource(R.string.pm),
                        )
                    }
                }
            }
            Box(Modifier.width(1.dp).fillMaxHeight(.72f).background(c.textSecondary.copy(alpha = .28f)))
            SafaaNext(data, next, Modifier.weight(.85f).padding(start = s.widthDp(.025f)))
        }
    }
}

@Composable private fun SafaaNext(
    data: ScreenDisplayData,
    p: PrayerInfo,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column {
            TvAutoSizeText(
                localizedStringResource(R.string.next_prayer).uppercase(),
                c.uiTertiary,
                s.height(.08f),
                s.height(.04f),
                fontFamily = FontFamily.Serif,
                maxLines = 1,
            )
            TvAutoSizeText(
                p.name,
                c.textPrimary,
                s.height(.20f),
                s.height(.08f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            TvAutoSizeText(
                TimeUtils.formatTimeBasedOnPreference(p.azanTime, data.settings.show24HourFormat),
                c.primaryAccent,
                s.height(.16f),
                s.height(.08f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
            )
            p.iqamahTime?.let {
                TvAutoSizeText(
                    "${localizedStringResource(R.string.iqamah_letter)} ${
                        TimeUtils.formatTimeBasedOnPreference(it, data.settings.show24HourFormat)
                    }",
                    c.textPrimary,
                    s.height(.14f),
                    s.height(.07f),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable private fun SafaaDial(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val hourAngle by
        rememberClockwiseHandAngle(
            clockHourAngle(data.clock.hour12, data.clock.minute, data.clock.second),
            "safaa_hour",
        )
    val minuteAngle by
        rememberClockwiseHandAngle(
            clockMinuteAngle(data.clock.minute, data.clock.second),
            "safaa_minute",
        )
    val secondAngle by rememberClockwiseHandAngle(clockSecondAngle(data.clock.second), "safaa_second")
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension * .46f
        drawRegulatorBezel(center, r, c.backgroundMain, c.textPrimary, c.textSecondary)
        drawMinuteTickRing(center, r * .90f, r * .84f, c.textPrimary)
        drawRegulatorMinuteMarks(center, r * 1.04f, c.textPrimary)
        drawRomanNumerals(center, r, c.textPrimary, textScale = 0.16f)
        drawClockHand(center, r * .48f, hourAngle, c.textPrimary, r * .040f)
        drawClockHand(center, r * .73f, minuteAngle, c.textPrimary, r * .024f)
        if (data.clock.showSeconds) {
            drawClockHand(center, r * .80f, secondAngle, c.primaryAccent, r * .005f)
        }
        drawCircle(c.primaryAccent, r * .022f, center)
    }
}

@Composable private fun SafaaTable(
    data: ScreenDisplayData,
    rows: List<PrayerInfo>,
    next: PrayerType?,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.padding(top = 10.dp)) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(s.widthDp(.04f))) {
            listOf(rows.take(3), rows.drop(3)).forEach { group ->
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(
                        Modifier.fillMaxWidth().height(s.heightDp(.15f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.weight(1.1f))
                        listOf(R.string.azan, R.string.iqamah_letter).forEach { label ->
                            TvAutoSizeText(
                                localizedStringResource(label), c.textSecondary,
                                s.height(.085f), s.height(.045f),
                                Modifier.weight(1f), FontFamily.Serif,
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                    group.forEach { prayer ->
                        val isNext = prayer.type == next
                        val color = when {
                            prayer.type == PrayerType.SUNRISE -> c.uiTertiary
                            isNext -> c.primaryAccent
                            else -> c.textPrimary
                        }
                        Row(
                            Modifier.weight(1f).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TvAutoSizeText(
                                prayer.name, if (isNext) c.primaryAccent else c.textPrimary,
                                s.height(.17f), s.height(.075f),
                                Modifier.weight(1.1f), FontFamily.Serif,
                                if (isNext) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                            )
                            TvAutoSizeText(
                                TimeUtils.formatTimeBasedOnPreference(prayer.azanTime, data.settings.show24HourFormat),
                                color, s.height(.18f), s.height(.085f),
                                Modifier.weight(1f).padding(start = s.widthDp(.005f)),
                                FontFamily.Serif, FontWeight.Bold,
                                textAlign = TextAlign.End,
                            )
                            TvAutoSizeText(
                                prayer.iqamahTime?.let {
                                    TimeUtils.formatTimeBasedOnPreference(it, data.settings.show24HourFormat)
                                }.orEmpty(),
                                if (isNext) c.primaryAccent else c.textSecondary,
                                s.height(.18f), s.height(.085f),
                                Modifier.weight(1f).padding(start = s.widthDp(.01f)),
                                FontFamily.Serif, FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(c.textSecondary.copy(alpha = .18f)))
                    }
                }
            }
        }
    }
}

@Composable private fun SafaaAzanHour(
    data: ScreenDisplayData,
    p: PrayerInfo,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val cd = inlineCountdown(data, p)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TvAutoSizeText(
                prayerCountdownInHeading(p, cd.isIqamah).uppercase(),
                c.uiTertiary,
                s.height(.075f),
                s.height(.04f),
                Modifier.fillMaxWidth(.94f),
                fontFamily = FontFamily.Serif,
                letterSpacing = s.height(.012f),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            TvAutoSizeText(
                "%02d:%02d".format(cd.minutes, cd.seconds),
                c.textPrimary,
                s.height(.52f),
                s.height(.30f),
                Modifier.padding(top = s.heightDp(.008f)),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeightRatio = 1.02f,
                maxLines = 1,
            )
            Box(
                Modifier
                    .padding(top = s.heightDp(.012f))
                    .fillMaxWidth(.42f)
                    .height(2.dp)
                    .background(c.textSecondary.copy(alpha = 0.35f)),
            )
            PrayerStatusLine(
                data,
                p,
                c.textSecondary,
                s.height(.075f),
                s.height(.042f),
                Modifier
                    .fillMaxWidth(.96f)
                    .padding(top = s.heightDp(.012f)),
                fontFamily = FontFamily.Serif,
            )
        }
    }
}

@Composable private fun SafaaSixColumnRail(
    data: ScreenDisplayData,
    rows: List<PrayerInfo>,
    next: PrayerType?,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.padding(top = 8.dp)) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(c.textPrimary),
            )
            Row(Modifier.fillMaxSize().padding(top = s.heightDp(.012f))) {
                rows.forEachIndexed { index, p ->
                    val isNext = p.type == next
                    val isSunrise = p.type == PrayerType.SUNRISE
                    if (index > 0) {
                        Box(Modifier.width(1.dp).fillMaxHeight(.72f).align(Alignment.CenterVertically)
                            .background(c.textSecondary.copy(alpha = .20f)))
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = s.widthDp(.006f)),
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
                            s.height(.18f),
                            s.height(.10f),
                            fontFamily = FontFamily.Serif,
                            fontWeight = if (isNext) FontWeight.Bold else null,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                        TvAutoSizeText(
                            TimeUtils.formatTimeBasedOnPreference(p.azanTime, data.settings.show24HourFormat),
                            when {
                                isNext -> c.textPrimary
                                isSunrise -> c.uiTertiary
                                else -> c.textPrimary
                            },
                            s.height(.26f),
                            s.height(.13f),
                            Modifier.padding(top = s.heightDp(.006f)),
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                        Box(Modifier.fillMaxWidth().height(s.heightDp(.20f)), contentAlignment = Alignment.Center) {
                            p.iqamahTime?.let { time ->
                                TvAutoSizeText(
                                    "${localizedStringResource(R.string.iqamah_letter)} " +
                                        TimeUtils.formatTimeBasedOnPreference(time, data.settings.show24HourFormat),
                                    c.textSecondary, s.height(.115f), s.height(.06f),
                                    fontFamily = FontFamily.Serif,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun SafaaCountdown(
    data: ScreenDisplayData,
    v: FullScreenCountdownData,
    modifier: Modifier,
) = SafaaScene(
    modifier,
    fullScreenCountdownTitle(v.prayerName, v.prayerType, v.isIqamah, v.kind),
    "%02d:%02d".format(v.minutes, v.seconds),
    TimeUtils.formatTimeBasedOnPreference(v.targetTime, data.settings.show24HourFormat),
)

@Composable private fun SafaaBayan(
    v: JummahData,
    modifier: Modifier,
) = SafaaScene(
    modifier,
    localizedStringResource(R.string.jummah_in_progress_title),
    v.formattedRemaining(),
    localizedStringResource(R.string.jummah_prayer_reminder),
)

@Composable private fun SafaaDua(modifier: Modifier) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain), contentAlignment = Alignment.Center) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(
            Modifier.fillMaxWidth(.86f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(s.heightDp(.035f)),
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.textSecondary))
            TvAutoSizeText(
                localizedStringResource(R.string.silent_your_phone).uppercase(),
                c.uiTertiary,
                s.height(.05f),
                s.height(.025f),
                fontFamily = FontFamily.Serif,
                letterSpacing = s.height(.008f),
            )
            TvAutoSizeText(
                localizedStringResource(R.string.dua_joining_saff_arabic),
                c.textPrimary,
                s.height(.15f),
                s.height(.075f),
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            TvAutoSizeText(
                localizedStringResource(R.string.dua_joining_saff_transliteration),
                c.primaryAccent,
                s.height(.065f),
                s.height(.032f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            TvAutoSizeText(
                localizedStringResource(R.string.dua_joining_saff_meaning),
                c.textSecondary,
                s.height(.05f),
                s.height(.025f),
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
    }
}

@Composable private fun SafaaScene(
    modifier: Modifier,
    title: String,
    hero: String,
    footer: String,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain), contentAlignment = Alignment.Center) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxWidth(.72f), horizontalAlignment = Alignment.CenterHorizontally) {
            TvAutoSizeText(
                title,
                c.primaryAccent,
                s.height(.085f),
                s.height(.042f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            TvAutoSizeText(
                hero,
                c.textPrimary,
                s.height(.36f),
                s.height(.19f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Box(Modifier.width(s.widthDp(.28f)).height(1.dp).background(c.textSecondary))
            TvAutoSizeText(
                footer,
                c.textSecondary,
                s.height(.12f),
                s.height(.065f),
                Modifier.padding(top = 12.dp),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
    }
}
