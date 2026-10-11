package com.mosque.prayerclock.ui.components.screenfaces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.components.*
import com.mosque.prayerclock.ui.components.screenfaces.shared.*
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.LocalColorTheme
import com.mosque.prayerclock.ui.theme.AppFonts
import com.mosque.prayerclock.utils.TimeUtils
import com.mosque.prayerclock.viewmodel.MainUiState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object KaashScreenFace : ScreenFace {
    override val id = "kaash"
    override val name = "Kaash"
    override val description = "Modular bento layout on a twelve-column grid"
    override val recommendedColorThemeId = "ink_saffron"

    @Composable override fun Dashboard(
        data: ScreenDisplayData,
        callbacks: ScreenFaceCallbacks,
        modifier: Modifier,
    ) = KaashDashboard(data, callbacks, modifier)

    @Composable override fun Countdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) = KaashCountdown(data, countdown, modifier)

    @Composable override fun FridayNightBayanCountdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) = KaashCountdown(data, countdown, modifier)

    @Composable override fun Dua(
        data: ScreenDisplayData,
        modifier: Modifier,
    ) = KaashDua(modifier)

    @Composable override fun FridayBayan(
        data: ScreenDisplayData,
        bayan: JummahData,
        modifier: Modifier,
    ) = KaashBayan(bayan, modifier)
}

@Composable private fun KaashTile(
    modifier: Modifier,
    accent: Boolean = false,
    contentPadding: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalColorTheme.current
    Box(
        modifier
            .background(
                if (accent) c.primaryAccent else c.surfaceCard,
                RoundedCornerShape(12.dp),
            ).border(
                1.dp,
                if (accent) c.primaryAccent else c.surfaceSecondary,
                RoundedCornerShape(12.dp),
            ).padding(contentPadding),
        contentAlignment = Alignment.CenterStart,
        content = content,
    )
}

@Composable private fun KaashDashboard(
    data: ScreenDisplayData,
    cb: ScreenFaceCallbacks,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain).padding(10.dp)) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            KaashHeader(data, Modifier.height(s.heightDp(.14f)).fillMaxWidth())
            when (val state = data.mainState) {
                is MainUiState.Success -> {
                    val rows = prayerRows(state.prayerTimes, data.currentTime)
                    val next = resolveDisplayNextPrayer(rows, state.nextPrayer, data.currentTime, state.nextDayFajr)
                    if (data.isCountdownVisibleForWeight) {
                        KaashHour(
                            data,
                            next,
                            Modifier.weight(.66f).fillMaxWidth().clickable(onClick = cb.onOpenSettings),
                        )
                    } else {
                        KaashPrimary(
                            data,
                            next,
                            Modifier.weight(.66f).fillMaxWidth().clickable(onClick = cb.onOpenSettings),
                        )
                    }
                    KaashPrayerTiles(data, rows, next.type, Modifier.weight(.25f).fillMaxWidth())
                }

                else -> {
                    KaashTile(Modifier.fillMaxSize()) {
                        TvAutoSizeText(data.errorMessage ?: "…", c.textPrimary, s.height(.08f))
                    }
                }
            }
        }
    }
}

@Composable private fun KaashHeader(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val h = headerData(data)
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KaashTile(Modifier.weight(1f).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(5.dp).fillMaxHeight(.62f).background(c.primaryAccent, RoundedCornerShape(8.dp)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        TvAutoSizeText(
                            h.mosque,
                            c.textPrimary,
                            s.height(.28f),
                            s.height(.16f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
            KaashTile(Modifier.weight(1f).fillMaxHeight()) {
                Column {
                    TvAutoSizeText(
                        h.hijri,
                        c.textPrimary,
                        s.height(.24f),
                        s.height(.13f),
                        maxLines = 1,
                    )
                    TvAutoSizeText(
                        "${h.weekday} · ${h.gregorian}",
                        c.textSecondary,
                        s.height(.15f),
                        s.height(.08f),
                        maxLines = 1,
                    )
                }
            }
            if (h.weatherAvailable) {
                KaashTile(Modifier.weight(.8f).fillMaxHeight(), contentPadding = 10.dp) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().weight(.6f), contentAlignment = Alignment.CenterStart) {
                            TvAutoSizeText(
                                h.temperature,
                                c.textPrimary,
                                s.height(.36f),
                                s.height(.20f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                        if (h.condition.isNotBlank()) {
                            Box(Modifier.fillMaxWidth().weight(.4f), contentAlignment = Alignment.CenterStart) {
                                TvAutoSizeText(
                                    h.condition,
                                    c.textSecondary,
                                    s.height(.22f),
                                    s.height(.12f),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun KaashPrimary(
    data: ScreenDisplayData,
    next: PrayerInfo,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val analog = kaashUsesAnalog(data)
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KaashTile(Modifier.weight(if (analog) 1.15f else 2f).fillMaxHeight()) {
                KaashClock(data, Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KaashTile(Modifier.weight(.72f).fillMaxWidth(), true) {
                    Column {
                        TvAutoSizeText(
                            localizedStringResource(R.string.next_prayer).uppercase(),
                            c.backgroundMain.copy(alpha = .7f),
                            s.height(.06f),
                            s.height(.03f),
                        )
                        TvAutoSizeText(
                            next.name,
                            c.backgroundMain,
                            s.height(.16f),
                            s.height(.07f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                        )
                        TvAutoSizeText(
                            TimeUtils.formatTimeBasedOnPreference(next.azanTime, data.settings.show24HourFormat),
                            c.backgroundMain,
                            s.height(.13f),
                            s.height(.06f),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (next.iqamahTime != null) {
                    KaashTile(Modifier.weight(.28f).fillMaxWidth()) {
                        TvAutoSizeText(
                            "${localizedStringResource(R.string.iqamah_letter)} ${
                                TimeUtils.formatTimeBasedOnPreference(next.iqamahTime, data.settings.show24HourFormat)
                            }",
                            c.textPrimary,
                            s.height(.10f),
                            s.height(.05f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private fun kaashUsesAnalog(data: ScreenDisplayData): Boolean =
    data.settings.clockType == ClockType.ANALOG ||
        data.settings.clockType == ClockType.BOTH && (data.clock.minute / 2) % 2 != 0

@Composable private fun KaashClock(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    if (kaashUsesAnalog(data)) {
        KaashDial(data, modifier)
    } else {
        BoxWithConstraints(modifier) {
            val s = rememberTvTextScale(maxWidth, maxHeight)
            Column(Modifier.fillMaxSize()) {
                TvAutoSizeText(
                    localizedStringResource(R.string.current_time).uppercase(),
                    c.textSecondary,
                    s.height(.06f),
                    s.height(.03f),
                    maxLines = 1,
                )
                FaceTickClock(
                    clock = data.clock,
                    primaryColor = c.textPrimary,
                    mutedColor = c.textSecondary,
                    style = FaceClockStyle.Regulator,
                    maxHeightFraction = .94f,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    fontWeight = FontWeight.Bold,
                    secondaryScale = .38f,
                    verticalAlignment = Alignment.CenterVertically,
                    amLabel = localizedStringResource(R.string.am),
                    pmLabel = localizedStringResource(R.string.pm),
                )
            }
        }
    }
}

@Composable private fun KaashDial(
    data: ScreenDisplayData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val hourAngle by
        rememberClockwiseHandAngle(
            clockHourAngle(data.clock.hour12, data.clock.minute, data.clock.second),
            "kaash_hour",
        )
    val minuteAngle by
        rememberClockwiseHandAngle(
            clockMinuteAngle(data.clock.minute, data.clock.second),
            "kaash_minute",
        )
    val secondAngle by rememberClockwiseHandAngle(clockSecondAngle(data.clock.second), "kaash_second")
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val radiusPx = with(density) { minOf(maxWidth.toPx(), maxHeight.toPx()) * .47f }
        val textMeasurer = rememberTextMeasurer()
        val numeralSize = with(density) { (radiusPx * .17f).toSp() }
        // Measure labels only when the dial size or typography changes, not on hand-animation frames.
        val numerals = remember(textMeasurer, numeralSize, c.textPrimary) {
            (1..12).map { hour ->
                textMeasurer.measure(
                    hour.toString(),
                    TextStyle(
                        fontFamily = AppFonts.Display,
                        fontSize = numeralSize,
                        fontWeight = FontWeight.SemiBold,
                        color = c.textPrimary,
                    ),
                    maxLines = 1,
                )
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            if (size.minDimension <= 0f) return@Canvas
            val center = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension * .47f
            drawCircle(c.backgroundMain, r, center)
            drawCircle(c.primaryAccent.copy(alpha = .3f), r, center, style = Stroke(r * .008f))
            repeat(60) { index ->
                val angle = index / 60f * 2f * PI.toFloat() - PI.toFloat() / 2f
                val direction = Offset(cos(angle), sin(angle))
                val major = index % 5 == 0
                drawLine(
                    if (major) c.primaryAccent else c.textSecondary.copy(alpha = .6f),
                    center + direction * (r * if (major) .87f else .92f),
                    center + direction * (r * .96f),
                    strokeWidth = r * if (major) .018f else .006f,
                    cap = StrokeCap.Round,
                )
            }
            numerals.forEachIndexed { index, label ->
                val angle = (index + 1) / 12f * 2f * PI.toFloat() - PI.toFloat() / 2f
                val position = center + Offset(cos(angle), sin(angle)) * (r * .74f)
                drawText(
                    label,
                    topLeft = position - Offset(label.size.width / 2f, label.size.height / 2f),
                )
            }
            drawClockHand(center, r * .48f, hourAngle, c.textPrimary, r * .045f)
            drawClockHand(center, r * .68f, minuteAngle, c.textPrimary, r * .025f)
            if (data.clock.showSeconds) {
                drawClockHand(center, r * .83f, secondAngle, c.primaryAccent, r * .009f)
            }
            drawCircle(c.primaryAccent, r * .035f, center)
            drawCircle(c.backgroundMain, r * .012f, center)
        }
    }
}

@Composable private fun KaashPrayerTiles(
    data: ScreenDisplayData,
    rows: List<PrayerInfo>,
    next: PrayerType?,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { p ->
                KaashTile(
                    Modifier.weight(1f).fillMaxHeight(),
                    accent =
                        p.type == next,
                ) {
                    Column {
                        TvAutoSizeText(
                            p.name,
                            if (p.type ==
                                next
                            ) {
                                c.backgroundMain
                            } else {
                                c.textSecondary
                            },
                            s.height(.19f),
                            s.height(.08f),
                            fontWeight =
                                if (p.type ==
                                    next
                                ) {
                                    FontWeight.Bold
                                } else {
                                    null
                                },
                            maxLines = 2,
                        )
                        ;TvAutoSizeText(
                            TimeUtils.formatTimeBasedOnPreference(p.azanTime, data.settings.show24HourFormat),
                            if (p.type ==
                                next
                            ) {
                                c.backgroundMain
                            } else if (p.type ==
                                PrayerType.SUNRISE
                            ) {
                                c.uiTertiary
                            } else {
                                c.textPrimary
                            },
                            s.height(.22f),
                            s.height(.10f),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun KaashHour(
    data: ScreenDisplayData,
    p: PrayerInfo,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    val cd = inlineCountdown(data, p)
    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KaashTile(Modifier.weight(1f).fillMaxHeight()) {
                KaashClock(data, Modifier.fillMaxSize())
            }
            KaashTile(Modifier.weight(1f).fillMaxHeight(), true) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(.18f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        TvAutoSizeText(
                            prayerWithEventLabel(p, cd.isIqamah),
                            c.backgroundMain,
                            s.height(.09f),
                            s.height(.035f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                        )
                    }
                    Box(Modifier.weight(.68f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        TvAutoSizeText(
                            "%02d:%02d".format(cd.minutes, cd.seconds),
                            c.backgroundMain,
                            s.height(.62f),
                            s.height(.28f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    Box(Modifier.weight(.14f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        TvAutoSizeText(
                            TimeUtils.formatTimeBasedOnPreference(cd.targetTime, data.settings.show24HourFormat),
                            c.backgroundMain.copy(alpha = .75f), s.height(.07f), s.height(.035f),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun KaashCountdown(
    data: ScreenDisplayData,
    v: FullScreenCountdownData,
    modifier: Modifier,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain).padding(10.dp)) {
        KaashTile(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val s = rememberTvTextScale(maxWidth, maxHeight)
                Column(
                    Modifier.fillMaxSize().padding(
                        horizontal = s.widthDp(.035f),
                        vertical = s.heightDp(.025f),
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(s.heightDp(.025f)),
                ) {
                    Box(
                        Modifier.fillMaxWidth().weight(.20f),
                        contentAlignment = Alignment.Center,
                    ) {
                        TvAutoSizeText(
                            fullScreenCountdownTitle(v.prayerName, v.prayerType, v.isIqamah, v.kind),
                            c.primaryAccent,
                            s.height(.13f),
                            s.height(.045f),
                            modifier = Modifier.fillMaxWidth(),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().weight(.64f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(v.minutes, v.seconds).forEachIndexed { index, value ->
                            KaashTile(Modifier.weight(1f).fillMaxHeight(), true) {
                                BoxWithConstraints(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val digits = rememberTvTextScale(maxWidth, maxHeight)
                                    TvAutoSizeText(
                                        "%02d".format(value),
                                        c.backgroundMain,
                                        digits.height(.90f),
                                        digits.height(.40f),
                                        modifier = Modifier.fillMaxWidth(),
                                        fontFamily = AppFonts.Display,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                    )
                                }
                            }
                            if (index == 0) {
                                Box(
                                    Modifier.weight(.16f).fillMaxHeight(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TvAutoSizeText(
                                        ":",
                                        c.textPrimary,
                                        s.height(.34f),
                                        modifier = Modifier.fillMaxWidth(),
                                        fontFamily = AppFonts.Display,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        Modifier.fillMaxWidth().weight(.16f),
                        contentAlignment = Alignment.Center,
                    ) {
                        TvAutoSizeText(
                            TimeUtils.formatTimeBasedOnPreference(v.targetTime, data.settings.show24HourFormat),
                            c.textSecondary,
                            s.height(.11f),
                            s.height(.045f),
                            modifier = Modifier.fillMaxWidth(),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun KaashBayan(
    v: JummahData,
    modifier: Modifier,
) = KaashTextScene(
    modifier,
    localizedStringResource(R.string.jummah_in_progress_title),
    v.formattedRemaining(),
    localizedStringResource(R.string.jummah_prayer_reminder),
)

@Composable private fun KaashDua(modifier: Modifier) =
    KaashTextScene(
        modifier,
        localizedStringResource(R.string.silent_your_phone),
        localizedStringResource(R.string.dua_joining_saff_arabic),
        localizedStringResource(R.string.dua_joining_saff_meaning),
        true,
    )

@Composable private fun KaashTextScene(
    modifier: Modifier,
    title: String,
    hero: String,
    body: String,
    dua: Boolean = false,
) {
    val c = LocalColorTheme.current
    BoxWithConstraints(modifier.background(c.backgroundMain).padding(10.dp)) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            KaashTile(Modifier.fillMaxWidth().weight(.12f), true) {
                TvAutoSizeText(
                    title.uppercase(),
                    c.backgroundMain,
                    s.height(.05f),
                    s.height(.025f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            KaashTile(Modifier.fillMaxWidth().weight(.88f)) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    TvAutoSizeText(
                        hero,
                        c.textPrimary,
                        s.height(if (dua) .15f else .28f),
                        s.height(if (dua) .075f else .15f),
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (dua) AppFonts.Arabic else AppFonts.Display,
                        textAlign = TextAlign.Center,
                        maxLines = if (dua) 2 else 1,
                    )
                    if (dua) {
                        TvAutoSizeText(
                            localizedStringResource(R.string.dua_joining_saff_transliteration),
                            c.primaryAccent,
                            s.height(.065f),
                            s.height(.032f),
                            Modifier.padding(top = s.heightDp(.02f)),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                    TvAutoSizeText(
                        body,
                        c.textSecondary,
                        s.height(if (dua) .05f else .085f),
                        s.height(if (dua) .025f else .045f),
                        Modifier.padding(top = 14.dp),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                    )
                }
            }
        }
    }
}
