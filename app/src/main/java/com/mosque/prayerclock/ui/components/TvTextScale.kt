package com.mosque.prayerclock.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * Resolution-independent typography measurements for TV surfaces.
 *
 * Sizes are calculated from the actual pixels assigned to a composable and converted back to sp.
 * This mirrors the proven Classic calculations and avoids fixed-sp plateaus where a 1080p region
 * accidentally renders text at the same (or a smaller) pixel size than its 720p equivalent.
 */
@Immutable
data class TvTextScale internal constructor(
    private val widthPx: Float,
    private val heightPx: Float,
    private val density: Density,
) {
    fun height(fraction: Float): TextUnit = with(density) { (heightPx * fraction).toSp() }

    fun width(characterSlots: Float, glyphWidthFactor: Float = 1f): TextUnit = with(density) {
        (widthPx / characterSlots.coerceAtLeast(1f) * glyphWidthFactor).toSp()
    }

    fun fit(
        heightFraction: Float,
        characterSlots: Float,
        glyphWidthFactor: Float = 1f,
    ): TextUnit = with(density) {
        min(
            heightPx * heightFraction,
            widthPx / characterSlots.coerceAtLeast(1f) * glyphWidthFactor,
        ).toSp()
    }

    fun heightDp(fraction: Float): Dp = with(density) { (heightPx * fraction).toDp() }

    fun widthDp(fraction: Float): Dp = with(density) { (widthPx * fraction).toDp() }
}

@Composable
fun rememberTvTextScale(availableWidth: Dp, availableHeight: Dp): TvTextScale {
    val density = LocalDensity.current
    return remember(availableWidth, availableHeight, density) {
        TvTextScale(
            widthPx = with(density) { availableWidth.toPx() },
            heightPx = with(density) { availableHeight.toPx() },
            density = density,
        )
    }
}

/**
 * Fits text to the assigned region before the first visible frame — no post-layout shrink steps.
 */
@Composable
fun TvAutoSizeText(
    text: String,
    color: Color,
    maxFontSize: TextUnit,
    minFontSize: TextUnit = maxFontSize * 0.5f,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeightRatio: Float = 1.12f,
    shadow: Shadow? = null,
) {
    val inheritedStyle = LocalTextStyle.current
    val effectiveStyle = inheritedStyle.copy(
        fontFamily = fontFamily ?: inheritedStyle.fontFamily,
        fontWeight = fontWeight ?: inheritedStyle.fontWeight,
        letterSpacing = if (letterSpacing.isUnspecified) inheritedStyle.letterSpacing else letterSpacing,
        shadow = shadow,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx =
            with(density) {
                if (maxHeight == Dp.Infinity) Float.POSITIVE_INFINITY else maxHeight.toPx()
            }
        val fontSize =
            remember(
                text,
                maxFontSize,
                minFontSize,
                maxLines,
                widthPx,
                heightPx,
                effectiveStyle,
                lineHeightRatio,
            ) {
                if (widthPx <= 0f) {
                    minFontSize
                } else {
                    fitAutoSizeFont(
                        text = text,
                        maxFontSize = maxFontSize,
                        minFontSize = minFontSize,
                        maxLines = maxLines,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        textMeasurer = textMeasurer,
                        styleBase = effectiveStyle,
                        lineHeightRatio = lineHeightRatio,
                    )
                }
            }
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = color,
            fontSize = fontSize,
            lineHeight = fontSize * lineHeightRatio,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            maxLines = maxLines,
            softWrap = maxLines > 1,
            overflow = TextOverflow.Clip,
            letterSpacing = letterSpacing,
            style = effectiveStyle,
        )
    }
}

private fun fitAutoSizeFont(
    text: String,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    maxLines: Int,
    widthPx: Float,
    heightPx: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    styleBase: TextStyle,
    lineHeightRatio: Float,
): TextUnit {
    val maxSp = maxFontSize.value
    val minSp = minFontSize.value.coerceAtMost(maxSp)
    if (text.isEmpty()) return maxFontSize

    fun fits(sizeSp: Float): Boolean {
        val result =
            textMeasurer.measure(
                text = text,
                style =
                    styleBase.copy(
                        fontSize = sizeSp.sp,
                        lineHeight = (sizeSp * lineHeightRatio).sp,
                    ),
                maxLines = maxLines,
                softWrap = maxLines > 1,
                overflow = TextOverflow.Clip,
                constraints =
                    Constraints(
                        maxWidth = widthPx.toInt().coerceAtLeast(1),
                    ),
            )
        // Measure the full lines before checking height. A bounded measurement can truncate a
        // wrapped localized label before the fitter sees how much vertical space it really needs.
        val fitsWidth = result.size.width <= widthPx && !result.hasVisualOverflow
        val fitsHeight =
            !heightPx.isFinite() ||
                (result.size.height <= heightPx && !result.didOverflowHeight)
        return fitsWidth && fitsHeight
    }

    // Preserve the preferred minimum when it fits. Long names in narrow tiles may need to
    // shrink further so the complete label remains visible.
    var low = if (fits(minSp)) minSp else minOf(1f, minSp)
    var high = maxSp
    var best = low
    while (high - low > 0.5f) {
        val mid = (low + high) / 2f
        if (fits(mid)) {
            best = mid
            low = mid
        } else {
            high = mid
        }
    }
    return best.sp
}
