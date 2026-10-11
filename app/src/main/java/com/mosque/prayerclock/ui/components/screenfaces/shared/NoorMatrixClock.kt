package com.mosque.prayerclock.ui.components.screenfaces.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.ui.components.clockfaces.ClockDisplayData
import com.mosque.prayerclock.ui.components.rememberTvTextScale

/**
 * Noor phosphor-board clock: instant tick-step digit changes and a blinking colon,
 * with smaller seconds and AM/PM on the same row.
 */
@Composable
fun NoorMatrixClock(
    clock: ClockDisplayData,
    color: Color,
    maxHeightFraction: Float,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Monospace,
    fontWeight: FontWeight = FontWeight.Bold,
    shadow: Shadow? = null,
    amLabel: String = "AM",
    pmLabel: String = "PM",
    secondaryScale: Float = .45f,
) {
    val hourText =
        if (clock.show24Hour) {
            "%02d".format(clock.hour24)
        } else {
            "%d".format(if (clock.hour12 == 0) 12 else clock.hour12)
        }
    val minuteText = "%02d".format(clock.minute)
    val secondText = "%02d".format(clock.second)
    val period = if (clock.show24Hour) "" else if (clock.isAm) amLabel else pmLabel
    val sizingSegments = remember(clock.show24Hour, clock.showSeconds, amLabel, pmLabel, secondaryScale) {
        val widestPeriod = if (amLabel.length >= pmLabel.length) amLabel else pmLabel
        listOf(
            "88:88" to 1f,
            ((if (clock.showSeconds) ":88" else "") +
                (if (clock.show24Hour) "" else " $widestPeriod")) to secondaryScale,
        )
    }

    BoxWithConstraints(modifier) {
        val s = rememberTvTextScale(maxWidth, maxHeight)
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val heightCapSp = s.fit(maxHeightFraction, 1f, 1.62f).value
        val fontSize =
            remember(sizingSegments, widthPx, heightPx, heightCapSp, fontFamily, fontWeight, density, textMeasurer) {
                fitMatrixClockFontSize(
                    segments = sizingSegments,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    heightCapSp = heightCapSp,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    textMeasurer = textMeasurer,
                    fallback = s.fit(maxHeightFraction, 9f, 1.62f),
                )
            }
        val style =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontSize = fontSize,
                lineHeight = fontSize,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                shadow = shadow,
            )
        val colonAlpha = if (clock.second % 2 == 0) 1f else 0.2f
        val secondaryStyle = style.copy(fontSize = fontSize * secondaryScale, lineHeight = fontSize * secondaryScale)

        Box(
            Modifier.fillMaxWidth().fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MatrixSegment(hourText, color, style)
                MatrixColon(fontSize, color.copy(alpha = colonAlpha), style)
                MatrixSegment(minuteText, color, style)
                if (clock.showSeconds) {
                    MatrixColon(fontSize * secondaryScale, color.copy(alpha = colonAlpha), secondaryStyle)
                    MatrixSegment(secondText, color, secondaryStyle)
                }
                if (period.isNotEmpty()) MatrixSegment(" $period", color, secondaryStyle)
            }
        }
    }
}

@Composable
private fun MatrixSegment(
    text: String,
    color: Color,
    style: TextStyle,
) {
    Text(
        text = text,
        color = color,
        style = style,
        maxLines = 1,
        softWrap = false,
    )
}

@Composable
private fun MatrixColon(
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    baseStyle: TextStyle,
) {
    Text(
        text = ":",
        color = color,
        style = baseStyle.copy(fontSize = fontSize),
        maxLines = 1,
    )
}

private fun fitMatrixClockFontSize(
    segments: List<Pair<String, Float>>,
    widthPx: Float,
    heightPx: Float,
    heightCapSp: Float,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    fallback: androidx.compose.ui.unit.TextUnit,
): androidx.compose.ui.unit.TextUnit {
    if (widthPx <= 0f || heightPx <= 0f || heightCapSp <= 0f) return fallback
    val style = TextStyle(fontFamily = fontFamily, fontWeight = fontWeight)
    var low = 8f
    var high = heightCapSp
    var best = low
    while (high - low > 0.5f) {
        val mid = (low + high) / 2f
        val results = segments.filter { it.first.isNotEmpty() }.map { (text, scale) ->
            textMeasurer.measure(
                text = text,
                style = style.copy(fontSize = (mid * scale).sp, lineHeight = (mid * scale).sp),
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
