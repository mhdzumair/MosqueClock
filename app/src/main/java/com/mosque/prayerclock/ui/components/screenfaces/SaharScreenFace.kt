package com.mosque.prayerclock.ui.components.screenfaces

import com.mosque.prayerclock.ui.components.screenfaces.shared.PrayerDataUnavailable

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

object SaharScreenFace : ScreenFace {
    override val id = "sahar"
    override val name = "Sahar"
    override val description = "Clear daylight clock with an aligned prayer timetable"
    override val recommendedColorThemeId = "daylight_ivory"

    @Composable override fun Dashboard(
        data: ScreenDisplayData,
        callbacks: ScreenFaceCallbacks,
        modifier: Modifier,
    ) = SaharDashboard(data, callbacks, modifier)

    @Composable override fun Countdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) = SaharScene(
        modifier,
        fullScreenCountdownTitle(countdown.prayerName, countdown.prayerType, countdown.isIqamah, countdown.kind),
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
    ) = SaharDua(modifier)

    @Composable override fun FridayBayan(
        data: ScreenDisplayData,
        bayan: JummahData,
        modifier: Modifier,
    ) = SaharScene(
        modifier,
        localizedStringResource(R.string.jummah_in_progress_title),
        bayan.formattedRemaining(),
        localizedStringResource(R.string.jummah_prayer_reminder),
    )
}

@Composable private fun SaharDashboard(
    data: ScreenDisplayData,
    cb: ScreenFaceCallbacks,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain).padding(horizontal = 16.dp, vertical = 10.dp)) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize()) {
            SaharHeader(data, Modifier.height(s.heightDp(.14f)).fillMaxWidth())
            when (val state = data.mainState) {
                is MainUiState.Success -> {
                    val rows = prayerRows(state.prayerTimes, data.currentTime)
                    val next = resolveDisplayNextPrayer(rows, state.nextPrayer, data.currentTime, state.nextDayFajr)
                    if (data.isCountdownVisibleForWeight) {
                        SaharHour(
                            data,
                            next,
                            Modifier.weight(.58f).fillMaxWidth().clickable(onClick = cb.onOpenSettings),
                        )
                    } else {
                        SaharPrimary(
                            data,
                            next,
                            Modifier.weight(.58f).fillMaxWidth().clickable(onClick = cb.onOpenSettings),
                        )
                    }
                    SaharRail(data, rows, next.type, Modifier.weight(.28f).fillMaxWidth())
                }

                else -> PrayerDataUnavailable(data, cb, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable private fun SaharHeader(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val h = headerData(data)
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(s.widthDp(.025f)),
            ) {
                Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(s.heightDp(.08f))) {
                    TvAutoSizeText(
                        h.mosque.uppercase(), c.textPrimary, s.height(.36f), s.height(.16f),
                        fontWeight = FontWeight.Bold, maxLines = 1,
                    )
                    TvAutoSizeText(
                        "${h.weekday} · ${h.gregorian}", c.textSecondary, s.height(.25f), s.height(.12f),
                        maxLines = 1,
                    )
                }
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(s.heightDp(.08f)),
                ) {
                    TvAutoSizeText(
                        h.hijri,
                        c.textPrimary,
                        s.height(.30f),
                        s.height(.14f),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                    if (h.weatherAvailable) {
                        TvAutoSizeText(
                            buildString {
                                append(h.temperature)
                                if (h.humidity.isNotBlank()) {
                                    append(" · ")
                                    append(h.humidity)
                                }
                            },
                            c.textSecondary,
                            s.height(.25f),
                            s.height(.11f),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.textPrimary.copy(alpha = .25f)))
        }
    }
}

@Composable private fun SaharPrimary(
    data: ScreenDisplayData,
    next: PrayerInfo,
    modifier: Modifier,
    showCountdown: Boolean = false,
) {
    val c = LocalColorTheme.current
    val countdown = if (showCountdown) inlineCountdown(data, next) else null
    val analog =
        !showCountdown && (data.settings.clockType == ClockType.ANALOG ||
            data.settings.clockType == ClockType.BOTH && (data.clock.minute / 2) % 2 != 0)
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Row(
            Modifier.fillMaxSize().padding(vertical = s.heightDp(.065f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.widthDp(.025f)),
        ) {
            if (analog) {
                SaharDial(
                    data,
                    Modifier.weight(1.35f).fillMaxHeight(),
                )
            } else {
                Column(Modifier.weight(1.35f).fillMaxHeight()) {
                    TvAutoSizeText(
                        if (countdown != null) prayerWithEventLabel(next, countdown.isIqamah)
                        else localizedStringResource(R.string.current_time), c.textSecondary,
                        s.height(.065f), s.height(.035f), maxLines = 1,
                    )
                    if (countdown != null) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            TvAutoSizeText(
                                "%02d:%02d".format(countdown.minutes, countdown.seconds),
                                c.primaryAccent, s.height(.54f), s.height(.22f),
                                fontWeight = FontWeight.Bold, maxLines = 1,
                            )
                        }
                        FaceTickClock(
                            clock = data.clock, primaryColor = c.textSecondary, mutedColor = c.textSecondary,
                            style = FaceClockStyle.Uniform, maxHeightFraction = .10f,
                            amLabel = localizedStringResource(R.string.am),
                            pmLabel = localizedStringResource(R.string.pm),
                        )
                    } else FacePanelClock(
                        clock = data.clock,
                        primaryColor = c.textPrimary,
                        mutedColor = c.textSecondary,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        fontWeight = FontWeight.Bold,
                        secondaryScale = .34f,
                        amLabel = localizedStringResource(R.string.am),
                        pmLabel = localizedStringResource(R.string.pm),
                    )
                }
            }
            Box(Modifier.width(1.dp).fillMaxHeight(.85f).background(c.textSecondary.copy(alpha = .22f)))
            Column(
                Modifier.weight(1f).fillMaxHeight()
                    .background(c.primaryAccent.copy(alpha = .055f))
                    .padding(horizontal = s.widthDp(.018f), vertical = s.heightDp(.04f)),
                verticalArrangement = Arrangement.Center,
            ) {
                if (analog) {
                    FaceTickClock(
                        clock = data.clock,
                        primaryColor = c.textPrimary,
                        mutedColor = c.textSecondary,
                        style = FaceClockStyle.Uniform,
                        maxHeightFraction = .15f,
                        fontWeight = FontWeight.Bold,
                        amLabel = localizedStringResource(R.string.am),
                        pmLabel = localizedStringResource(R.string.pm),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(.85f)
                            .padding(vertical = s.heightDp(.018f))
                            .height(2.dp)
                            .background(c.textSecondary.copy(alpha = .22f)),
                    )
                }
                Box(Modifier.fillMaxWidth().weight(.16f), contentAlignment = Alignment.CenterStart) {
                    TvAutoSizeText(
                        localizedStringResource(R.string.next_prayer).uppercase(),
                        c.textSecondary,
                        s.height(.065f),
                        s.height(.04f),
                        maxLines = 1,
                    )
                }
                Box(Modifier.fillMaxWidth().weight(.38f), contentAlignment = Alignment.CenterStart) {
                    TvAutoSizeText(
                        next.name,
                        c.textPrimary,
                        s.height(.22f),
                        s.height(.09f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().weight(.34f),
                    horizontalArrangement = Arrangement.spacedBy(s.widthDp(.012f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SaharEventTime(
                        localizedStringResource(R.string.azan),
                        TimeUtils.formatTimeBasedOnPreference(next.azanTime, data.settings.show24HourFormat),
                        Modifier.weight(1f).fillMaxHeight(),
                        emphasized = true,
                    )
                    next.iqamahTime?.let {
                        SaharEventTime(
                            localizedStringResource(R.string.iqamah),
                            TimeUtils.formatTimeBasedOnPreference(it, data.settings.show24HourFormat),
                            Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun SaharEventTime(
    label: String,
    time: String,
    modifier: Modifier,
    emphasized: Boolean = false,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            TvAutoSizeText(label, c.textSecondary, s.height(.23f), s.height(.12f), maxLines = 1)
            Spacer(Modifier.height(s.heightDp(.06f)))
            TvAutoSizeText(
                time, if (emphasized) c.primaryAccent else c.textPrimary,
                s.height(.45f), s.height(.20f), fontWeight = FontWeight.SemiBold, maxLines = 1,
            )
        }
    }
}

@Composable private fun SaharDial(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val numeralPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    }
    val hourAngle by rememberClockwiseHandAngle(clockHourAngle(data.clock.hour12, data.clock.minute, data.clock.second), "sahar_hour")
    val minuteAngle by rememberClockwiseHandAngle(clockMinuteAngle(data.clock.minute, data.clock.second), "sahar_minute")
    val secondAngle by rememberClockwiseHandAngle(clockSecondAngle(data.clock.second), "sahar_second")
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension * .46f
        if (r <= 0f) return@Canvas

        // A complete bezel gives the dial a clear silhouette at TV viewing distances.
        drawCircle(c.surfaceCard, r, center)
        drawCircle(c.primaryAccent, r, center, style = Stroke(r * .018f))
        drawCircle(c.primaryAccent.copy(alpha = .18f), r * .965f, center, style = Stroke(r * .005f))

        repeat(60) { index ->
            val angle = index * 6f - 90f
            val hour = index % 5 == 0
            val cardinal = index % 15 == 0
            drawLine(
                color = when {
                    cardinal -> c.primaryAccent
                    hour -> c.textPrimary.copy(alpha = .85f)
                    else -> c.textSecondary.copy(alpha = .55f)
                },
                start = polarOffset(center, r * if (hour) .84f else .90f, angle),
                end = polarOffset(center, r * .935f, angle),
                strokeWidth = r * when {
                    cardinal -> .025f
                    hour -> .017f
                    else -> .006f
                },
                cap = StrokeCap.Round,
            )
        }

        drawIntoCanvas { canvas ->
            for (hour in 1..12) {
                val cardinal = hour % 3 == 0
                numeralPaint.color = (if (cardinal) c.primaryAccent else c.textPrimary).toArgb()
                numeralPaint.textSize = r * if (cardinal) .21f else .18f
                val point = polarOffset(center, r * .71f, hour * 30f - 90f)
                val baseline = point.y - (numeralPaint.ascent() + numeralPaint.descent()) / 2f
                canvas.nativeCanvas.drawText(hour.toString(), point.x, baseline, numeralPaint)
            }
        }

        drawLine(
            c.textPrimary,
            polarOffset(center, r * .10f, hourAngle + 180f),
            polarOffset(center, r * .48f, hourAngle),
            r * .055f, StrokeCap.Round,
        )
        drawLine(
            c.primaryAccent,
            polarOffset(center, r * .12f, minuteAngle + 180f),
            polarOffset(center, r * .81f, minuteAngle),
            r * .032f, StrokeCap.Round,
        )
        if (data.clock.showSeconds) {
            drawLine(
                c.secondaryAccent,
                polarOffset(center, r * .18f, secondAngle + 180f),
                polarOffset(center, r * .88f, secondAngle),
                r * .008f, StrokeCap.Round,
            )
            drawCircle(c.secondaryAccent, r * .025f, polarOffset(center, r * .14f, secondAngle + 180f))
        }
        drawCircle(c.primaryAccent, r * .045f, center)
        drawCircle(c.surfaceCard, r * .023f, center)
        drawCircle(c.textPrimary, r * .011f, center)
    }
}

@Composable private fun SaharRail(
    data: ScreenDisplayData,
    rows: List<PrayerInfo>,
    next: PrayerType?,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val now = data.currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val marker =
        (now.hour * 60 + now.minute) / 1440f
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column {
            Box(Modifier.fillMaxWidth().height(2.dp).background(c.textSecondary.copy(alpha = .22f))) {
                Box(Modifier.fillMaxWidth(marker).fillMaxHeight().background(c.primaryAccent))
                Box(Modifier.fillMaxWidth(marker).fillMaxHeight()) {
                    Box(
                        Modifier
                            .align(
                                Alignment.CenterEnd,
                            ).width(2.dp)
                            .height(s.heightDp(.08f))
                            .background(c.primaryAccent),
                    )
                }
            }
            Row(Modifier.fillMaxSize().padding(top = s.heightDp(.06f))) {
                rows.forEachIndexed { index, p ->
                    val highlighted = p.type == next
                    Column(
                        Modifier.weight(1f).fillMaxHeight()
                            .background(if (highlighted) c.primaryAccent.copy(alpha = .055f) else c.backgroundMain)
                            .padding(horizontal = s.widthDp(.008f), vertical = s.heightDp(.05f)),
                    ) {
                        Box(Modifier.weight(.36f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            TvAutoSizeText(
                                p.name,
                                if (highlighted) c.primaryAccent else c.textSecondary,
                                s.height(.20f),
                                s.height(.10f),
                                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 2,
                            )
                        }
                        Box(Modifier.weight(.34f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            TvAutoSizeText(
                                TimeUtils.formatTimeBasedOnPreference(p.azanTime, data.settings.show24HourFormat),
                                if (p.type == PrayerType.SUNRISE) c.uiTertiary else c.textPrimary,
                                s.height(.23f),
                                s.height(.12f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                        Box(Modifier.weight(.24f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            p.iqamahTime?.let {
                                TvAutoSizeText(
                                    "${localizedStringResource(R.string.iqamah_letter)} ${TimeUtils.formatTimeBasedOnPreference(it, data.settings.show24HourFormat)}",
                                    c.textSecondary, s.height(.13f), s.height(.08f), maxLines = 1,
                                )
                            }
                        }
                    }
                    if (index != rows.lastIndex) {
                        Box(Modifier.width(1.dp).fillMaxHeight(.9f).background(c.textSecondary.copy(alpha = .15f)))
                    }
                }
            }
        }
    }
}

@Composable private fun SaharHour(
    data: ScreenDisplayData,
    p: PrayerInfo,
    modifier: Modifier,
) = SaharPrimary(data, p, modifier, showCountdown = true)

@Composable private fun SaharScene(
    modifier: Modifier,
    title: String,
    hero: String,
    footer: String,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain), contentAlignment = Alignment.Center) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxWidth(.78f), horizontalAlignment = Alignment.CenterHorizontally) {
            TvAutoSizeText(
                title,
                c.primaryAccent,
                s.height(.085f),
                s.height(.042f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            TvAutoSizeText(
                hero,
                c.textPrimary,
                s.height(.34f),
                s.height(.18f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Box(Modifier.width(s.widthDp(.20f)).height(2.dp).background(c.primaryAccent))
            TvAutoSizeText(
                footer,
                c.textSecondary,
                s.height(.12f),
                s.height(.065f),
                Modifier.padding(top = 14.dp),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
    }
}

@Composable private fun SaharDua(modifier: Modifier) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain), contentAlignment = Alignment.Center) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(
            Modifier.fillMaxWidth(.86f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(s.heightDp(.03f)),
        ) {
            TvAutoSizeText(
                localizedStringResource(R.string.silent_your_phone).uppercase(),
                c.primaryAccent,
                s.height(.05f),
                s.height(.025f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            TvAutoSizeText(
                localizedStringResource(R.string.dua_joining_saff_arabic),
                c.textPrimary,
                s.height(.15f),
                s.height(.075f),
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
