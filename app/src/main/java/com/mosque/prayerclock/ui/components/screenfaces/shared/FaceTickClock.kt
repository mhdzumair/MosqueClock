package com.mosque.prayerclock.ui.components.screenfaces.shared

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.ui.components.clockfaces.ClockDisplayData
import com.mosque.prayerclock.ui.components.rememberTvTextScale
import com.mosque.prayerclock.ui.theme.AppFonts

enum class FaceClockStyle {
    Uniform,
    Regulator,
}

/** Fits the face's typography to its clock panel, with seconds and period inline. */
@Composable
fun FacePanelClock(
    clock: ClockDisplayData,
    primaryColor: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = AppFonts.Display,
    fontWeight: FontWeight = FontWeight.Bold,
    amLabel: String = "AM",
    pmLabel: String = "PM",
    secondaryScale: Float = .38f,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
) {
    FaceTickClock(
        clock = clock,
        primaryColor = primaryColor,
        mutedColor = mutedColor,
        style = FaceClockStyle.Regulator,
        maxHeightFraction = .94f,
        modifier = modifier,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        secondaryScale = secondaryScale,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        amLabel = amLabel,
        pmLabel = pmLabel,
    )
}

/**
 * Tick-step digital clock: stable sizing (no per-second shrink), blinking colon, digit-roll on change.
 * Font size is derived from the widest possible time string so seconds ticking never resize the row.
 */
@Composable
fun FaceTickClock(
    clock: ClockDisplayData,
    primaryColor: Color,
    mutedColor: Color,
    style: FaceClockStyle,
    maxHeightFraction: Float,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = AppFonts.Display,
    fontWeight: FontWeight = FontWeight.Bold,
    amLabel: String = "AM",
    pmLabel: String = "PM",
    lineHeightRatio: Float = 1.02f,
    secondaryScale: Float = 0.58f,
    verticalAlignment: Alignment.Vertical = Alignment.Bottom,
    shadow: Shadow? = null,
    showPeriod: Boolean = true,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
) {
    val hourText =
        if (clock.show24Hour) {
            "%02d".format(clock.hour24)
        } else {
            "%d".format(if (clock.hour12 == 0) 12 else clock.hour12)
        }
    val minuteText = "%02d".format(clock.minute)
    val secondText = "%02d".format(clock.second)
    val suffix = if (clock.show24Hour || !showPeriod) "" else if (clock.isAm) amLabel else pmLabel

    val sizingSegments =
        remember(clock.show24Hour, clock.showSeconds, style, amLabel, pmLabel, secondaryScale, showPeriod) {
            val period = if (amLabel.length >= pmLabel.length) amLabel else pmLabel
            val periodText = if (clock.show24Hour || !showPeriod) "" else " $period"
            when (style) {
                FaceClockStyle.Regulator -> listOf(
                    "88:88" to 1f,
                    ((if (clock.showSeconds) ":88" else "") + periodText) to secondaryScale,
                )
                FaceClockStyle.Uniform -> listOf(
                    ("88:88" + (if (clock.showSeconds) ":88" else "") + periodText) to 1f,
                )
            }
        }

    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val heightCapSp = s.fit(maxHeightFraction, 1f, 1.62f).value

        val mainFontSize =
            remember(sizingSegments, widthPx, heightPx, heightCapSp, fontFamily, fontWeight, lineHeightRatio, density, textMeasurer) {
                fitSingleLineFontSize(
                    segments = sizingSegments,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    heightCapSp = heightCapSp,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    lineHeightRatio = lineHeightRatio,
                    textMeasurer = textMeasurer,
                    fallback = s.fit(maxHeightFraction, sizingSegments.sumOf { it.first.length }.toFloat(), 1.62f),
                )
            }

        val secondarySize = mainFontSize * when (style) {
            FaceClockStyle.Uniform -> 1f
            FaceClockStyle.Regulator -> secondaryScale
        }
        val colonAlpha = if (clock.second % 2 == 0) 1f else 0.2f
        val rowHeight = with(density) { (mainFontSize.value * 1.08f).sp.toDp() }
        val rowModifier =
            if (verticalAlignment == Alignment.CenterVertically) {
                Modifier.fillMaxWidth().fillMaxHeight()
            } else {
                Modifier.fillMaxWidth().height(rowHeight)
            }

        Box(
            rowModifier,
            contentAlignment =
                if (verticalAlignment == Alignment.CenterVertically) {
                    Alignment.CenterStart
                } else {
                    Alignment.BottomStart
                },
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = verticalAlignment,
                horizontalArrangement = horizontalArrangement,
            ) {
            FaceClockSegment(
                hourText,
                mainFontSize,
                primaryColor,
                fontFamily,
                fontWeight,
                shadow,
                animate = true,
            )
            FaceClockColon(
                mainFontSize,
                primaryColor.copy(alpha = colonAlpha),
                fontFamily,
                fontWeight,
                shadow,
            )
            FaceClockSegment(
                minuteText,
                mainFontSize,
                primaryColor,
                fontFamily,
                fontWeight,
                shadow,
                animate = true,
            )
            if (clock.showSeconds) {
                FaceClockColon(
                    secondarySize,
                    mutedColor.copy(alpha = colonAlpha),
                    fontFamily,
                    fontWeight,
                    shadow,
                )
                FaceClockSegment(
                    secondText,
                    if (style == FaceClockStyle.Regulator) secondarySize else mainFontSize,
                    mutedColor,
                    fontFamily,
                    fontWeight,
                    shadow,
                    animate = false,
                )
            }
            if (suffix.isNotBlank()) {
                val suffixSize = if (style == FaceClockStyle.Regulator) secondarySize else mainFontSize
                FaceClockSegment(
                    suffix.trim(),
                    suffixSize,
                    if (style == FaceClockStyle.Regulator) mutedColor else primaryColor,
                    fontFamily,
                    fontWeight,
                    shadow,
                    animate = false,
                    leadingSpace = true,
                )
            }
            }
        }
    }
}

@Composable
private fun FaceClockColon(
    fontSize: TextUnit,
    color: Color,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    shadow: Shadow? = null,
) {
    Text(
        text = ":",
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        style = faceClockTextStyle(fontSize, fontFamily, fontWeight, shadow),
    )
}

@Composable
private fun FaceClockSegment(
    text: String,
    fontSize: TextUnit,
    color: Color,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    shadow: Shadow? = null,
    animate: Boolean,
    leadingSpace: Boolean = false,
) {
    val display = if (leadingSpace) " $text" else text
    val style = faceClockTextStyle(fontSize, fontFamily, fontWeight, shadow)
    if (!animate) {
        Text(
            text = display,
            color = color,
            style = style,
            maxLines = 1,
            softWrap = false,
        )
        return
    }
    Row(verticalAlignment = Alignment.Bottom) {
        display.forEach { char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    fadeIn(tween(120)) togetherWith fadeOut(tween(60))
                },
                label = "face_digit_roll",
            ) { digit ->
                Text(
                    text = digit.toString(),
                    color = color,
                    style = style,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

private fun faceClockTextStyle(
    fontSize: TextUnit,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    shadow: Shadow? = null,
): TextStyle =
    TextStyle(
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = fontSize,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        fontFeatureSettings = "tnum",
        shadow = shadow,
    )

private fun fitSingleLineFontSize(
    segments: List<Pair<String, Float>>,
    widthPx: Float,
    heightPx: Float,
    heightCapSp: Float,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    lineHeightRatio: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    fallback: TextUnit,
): TextUnit {
    if (widthPx <= 0f || heightPx <= 0f || heightCapSp <= 0f) return fallback
    var low = minOf(1f, heightCapSp)
    var high = heightCapSp
    var best = low
    while (high - low > 0.5f) {
        val mid = (low + high) / 2f
        val results = segments.filter { it.first.isNotEmpty() }.map { (text, scale) ->
            textMeasurer.measure(
                text = text,
                style = faceClockTextStyle((mid * scale).sp, fontFamily, fontWeight)
                    .copy(lineHeight = (mid * scale * lineHeightRatio).sp),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
        if (results.sumOf { it.size.width } <= widthPx && results.all { it.size.height <= heightPx }) {
            best = mid
            low = mid
        } else {
            high = mid
        }
    }
    return best.sp
}
