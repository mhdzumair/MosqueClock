package com.mosque.prayerclock.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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

/** Starts at the region's ideal size and reduces only when localized content actually overflows. */
@Composable
fun TvAutoSizeText(
    text: String,
    color: Color,
    maxFontSize: TextUnit,
    minFontSize: TextUnit = maxFontSize * 0.5f,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeightRatio: Float = 1.12f,
) {
    var fontSize by remember(text, maxFontSize, minFontSize, maxLines) { mutableStateOf(maxFontSize) }
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        lineHeight = fontSize * lineHeightRatio,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        overflow = TextOverflow.Clip,
        letterSpacing = letterSpacing,
        onTextLayout = { result ->
            if ((result.didOverflowWidth || result.didOverflowHeight) && fontSize > minFontSize) {
                val step = (maxFontSize.value * 0.025f).coerceAtLeast(0.5f)
                fontSize = (fontSize.value - step).coerceAtLeast(minFontSize.value).sp
            }
        },
    )
}
