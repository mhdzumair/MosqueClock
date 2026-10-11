package com.mosque.prayerclock.ui.components.screenfaces.shared

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A face-neutral progress primitive. Track shape and placement remain owned by each face. */
@Composable
fun AnimatedFaceProgress(
    progress: Float,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier,
) {
    // Progress ticks forward roughly once a second (driven by the clock), so a full-second linear
    // tween makes consecutive updates blend into one continuous rightward motion — like a real
    // download bar — instead of a short eased hop followed by a pause.
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = LinearEasing),
        label = "face_progress",
    )
    Canvas(modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val y = size.height / 2f
        val stroke = size.height.coerceAtLeast(1f)
        drawLine(trackColor, Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
        drawLine(progressColor, Offset(0f, y), Offset(size.width * animated, y), stroke, StrokeCap.Round)
    }
}

/** Progress rail with a round sun marker riding the fill edge — used on Ufuq progress bars. */
@Composable
fun AnimatedFaceSunProgress(
    progress: Float,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = LinearEasing),
        label = "face_sun_progress",
    )
    Canvas(modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val y = size.height / 2f
        val barStroke = (size.height * 0.18f).coerceAtLeast(2f).coerceAtMost(size.height)
        // Include the rays and their round caps when fitting the marker into a small rail.
        val markerRadius = minOf((size.height * 0.36f).coerceAtLeast(5f), size.minDimension / 4f)
        val markerInset = markerRadius * 2f
        val markerX = (size.width * animated).coerceIn(markerInset, size.width - markerInset)
        drawLine(trackColor, Offset(0f, y), Offset(size.width, y), barStroke, StrokeCap.Round)
        drawLine(progressColor, Offset(0f, y), Offset(markerX, y), barStroke, StrokeCap.Round)
        drawProgressSunMarker(Offset(markerX, y), markerRadius, progressColor)
    }
}

/** Compact sun disc + short rays sized to stay inside the progress bar bounds. */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProgressSunMarker(
    center: Offset,
    radius: Float,
    color: Color,
) {
    drawCircle(color, radius, center)
    repeat(8) { index ->
        val radians = index * 45f * PI.toFloat() / 180f
        val cos = cos(radians)
        val sin = sin(radians)
        val inner = radius * 1.2f
        val outer = radius * 1.65f
        drawLine(
            color = color,
            start = center + Offset(cos * inner, sin * inner),
            end = center + Offset(cos * outer, sin * outer),
            strokeWidth = (radius * 0.28f).coerceAtLeast(1.5f),
            cap = StrokeCap.Round,
        )
    }
}

/**
 * Keeps hand motion clockwise through 59s→0s. Read the returned state inside the draw lambda
 * so animation frames invalidate drawing rather than recomposing and measuring the dial.
 */
@Composable
fun rememberClockwiseHandAngle(baseAngle: Float, label: String): State<Float> {
    var targetAngle by remember(label) { mutableFloatStateOf(baseAngle) }
    LaunchedEffect(baseAngle) {
        var adjusted = baseAngle
        while (adjusted < targetAngle - 0.01f) {
            adjusted += 360f
        }
        targetAngle = adjusted
    }
    val animated = animateFloatAsState(
        targetAngle,
        tween(950, easing = LinearEasing),
        label = label,
    )
    return animated
}

fun clockHourAngle(hour12: Int, minute: Int, second: Int): Float =
    (hour12 * 30f) + (minute * 0.5f) + (second * 0.5f / 60f) - 90f

fun clockMinuteAngle(minute: Int, second: Int): Float =
    (minute * 6f) + (second * 0.1f) - 90f

fun clockSecondAngle(second: Int): Float = second * 6f - 90f

fun DrawScope.drawMinuteTickRing(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
    ticksPerRevolution: Int = 60,
    majorEvery: Int = 5,
) {
    repeat(ticksPerRevolution) { index ->
        val angle = index / ticksPerRevolution.toFloat() * 2f * PI.toFloat() - PI.toFloat() / 2f
        val cosA = cos(angle)
        val sinA = sin(angle)
        val major = index % majorEvery == 0
        drawLine(
            color = color.copy(alpha = if (major) 0.55f else 0.25f),
            start = Offset(center.x + cosA * innerRadius, center.y + sinA * innerRadius),
            end = Offset(center.x + cosA * outerRadius, center.y + sinA * outerRadius),
            strokeWidth = if (major) outerRadius * 0.012f else outerRadius * 0.006f,
            cap = StrokeCap.Round,
        )
    }
}

/** Printed minute ring used on Sahar — hairline ticks on a narrow band. */
fun DrawScope.drawPrintedMinuteRing(
    center: Offset,
    radius: Float,
    tickColor: Color,
) {
    val outer = radius * 0.98f
    val inner = radius * 0.93f
    repeat(60) { index ->
        val angle = index / 60f * 2f * PI.toFloat() - PI.toFloat() / 2f
        val cosA = cos(angle)
        val sinA = sin(angle)
        drawLine(
            tickColor,
            Offset(center.x + cosA * inner, center.y + sinA * inner),
            Offset(center.x + cosA * outer, center.y + sinA * outer),
            radius * 0.004f,
            StrokeCap.Round,
        )
    }
}

fun DrawScope.drawDayProgressArc(
    center: Offset,
    radius: Float,
    progress: Float,
    color: Color,
    strokeWidth: Float,
) {
    val sweep = progress.coerceIn(0f, 1f) * 360f
    if (sweep <= 0f) return
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
}

fun DrawScope.drawRomanNumerals(
    center: Offset,
    radius: Float,
    color: Color,
    textScale: Float = 0.10f,
) {
    val textSize = radius * textScale
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            textAlign = Paint.Align.CENTER
            this.textSize = textSize
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
    drawIntoCanvas { canvas ->
        listOf(
            "XII" to -90f,
            "III" to 0f,
            "VI" to 90f,
            "IX" to 180f,
        ).forEach { (label, degrees) ->
            val radians = degrees * PI.toFloat() / 180f
            val x = center.x + cos(radians) * radius * 0.78f
            val y = center.y + sin(radians) * radius * 0.78f
            val baseline = y - (paint.ascent() + paint.descent()) / 2f
            canvas.nativeCanvas.drawText(label, x, baseline, paint)
        }
    }
}

fun DrawScope.drawRegulatorBezel(
    center: Offset,
    radius: Float,
    dialColor: Color,
    outerRing: Color,
    innerRing: Color,
) {
    drawCircle(dialColor, radius, center)
    drawCircle(outerRing, radius, center, style = Stroke(radius * 0.014f))
    drawCircle(innerRing.copy(alpha = 0.35f), radius * 0.93f, center, style = Stroke(radius * 0.006f))
}

fun DrawScope.drawRegulatorMinuteMarks(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val outer = radius * 0.88f
    val inner = radius * 0.82f
    repeat(12) { index ->
        val angle = index / 12f * 2f * PI.toFloat() - PI.toFloat() / 2f
        val cosA = cos(angle)
        val sinA = sin(angle)
        val major = index % 3 == 0
        drawLine(
            color,
            Offset(center.x + cosA * inner, center.y + sinA * inner),
            Offset(center.x + cosA * outer, center.y + sinA * outer),
            if (major) radius * 0.018f else radius * 0.008f,
            StrokeCap.Round,
        )
    }
}

fun polarOffset(center: Offset, length: Float, degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    return Offset(
        center.x + cos(radians) * length,
        center.y + sin(radians) * length,
    )
}

enum class NoorRingKind {
    Seconds,
    Minutes,
    Hours,
}

fun DrawScope.drawNoorProgressRing(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
    kind: NoorRingKind,
) {
    val band = outerRadius - innerRadius
    val midRadius = (outerRadius + innerRadius) / 2f
    when (kind) {
        NoorRingKind.Seconds -> {
            repeat(60) { index ->
                val angle = index / 60f * 2f * PI.toFloat() - PI.toFloat() / 2f
                val cosA = cos(angle)
                val sinA = sin(angle)
                drawLine(
                    inactiveColor.copy(alpha = (inactiveColor.alpha * 1.1f).coerceAtMost(1f)),
                    Offset(center.x + cosA * innerRadius, center.y + sinA * innerRadius),
                    Offset(center.x + cosA * outerRadius, center.y + sinA * outerRadius),
                    band * 0.12f,
                    StrokeCap.Round,
                )
            }
        }
        NoorRingKind.Minutes -> {
            drawCircle(inactiveColor.copy(alpha = inactiveColor.alpha * 0.65f), midRadius, center, style = Stroke(band * 0.18f))
            repeat(12) { index ->
                val angle = index / 12f * 2f * PI.toFloat() - PI.toFloat() / 2f
                val cosA = cos(angle)
                val sinA = sin(angle)
                drawLine(
                    inactiveColor,
                    Offset(center.x + cosA * innerRadius, center.y + sinA * innerRadius),
                    Offset(center.x + cosA * outerRadius, center.y + sinA * outerRadius),
                    band * 0.22f,
                    StrokeCap.Round,
                )
            }
        }
        NoorRingKind.Hours -> {
            repeat(12) { index ->
                val angle = index / 12f * 2f * PI.toFloat() - PI.toFloat() / 2f
                val cosA = cos(angle)
                val sinA = sin(angle)
                val block = band * 0.55f
                val cx = center.x + cosA * midRadius
                val cy = center.y + sinA * midRadius
                drawRect(
                    inactiveColor.copy(alpha = (inactiveColor.alpha * 1.25f).coerceAtMost(1f)),
                    topLeft = Offset(cx - block / 2f, cy - block / 2f),
                    size = Size(block, block),
                )
            }
        }
    }
    val sweep = progress.coerceIn(0f, 1f) * 360f
    if (sweep <= 0f) return
    val strokeWidth =
        when (kind) {
            NoorRingKind.Seconds -> band * 0.92f
            NoorRingKind.Minutes -> band * 0.72f
            NoorRingKind.Hours -> band * 0.58f
        }
    val cap =
        when (kind) {
            NoorRingKind.Seconds -> StrokeCap.Butt
            NoorRingKind.Minutes -> StrokeCap.Round
            NoorRingKind.Hours -> StrokeCap.Round
        }
    drawArc(
        color = activeColor,
        startAngle = -90f,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - midRadius, center.y - midRadius),
        size = Size(midRadius * 2f, midRadius * 2f),
        style = Stroke(width = strokeWidth, cap = cap),
    )
}

fun DrawScope.drawNoorRingSeparator(
    center: Offset,
    radius: Float,
    gapColor: Color,
    width: Float,
) {
    drawCircle(gapColor, radius, center, style = Stroke(width = width))
}

fun DrawScope.drawNoorMatrixRing(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
    tickCount: Int = 60,
) {
    drawMinuteTickRing(center, outerRadius, innerRadius, inactiveColor, tickCount, 1)
    val sweep = progress.coerceIn(0f, 1f) * 360f
    if (sweep <= 0f) return
    val midRadius = (outerRadius + innerRadius) / 2f
    drawArc(
        color = activeColor,
        startAngle = -90f,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - midRadius, center.y - midRadius),
        size = Size(midRadius * 2f, midRadius * 2f),
        style = Stroke(width = outerRadius - innerRadius, cap = StrokeCap.Butt),
    )
}

fun DrawScope.drawNoorHourBlocks(
    center: Offset,
    radius: Float,
    blockSize: Float,
    color: Color,
) {
    repeat(12) { index ->
        val angle = index / 12f * 2f * PI.toFloat() - PI.toFloat() / 2f
        val cosA = cos(angle)
        val sinA = sin(angle)
        val cx = center.x + cosA * radius
        val cy = center.y + sinA * radius
        drawRect(
            color = color,
            topLeft = Offset(cx - blockSize / 2f, cy - blockSize / 2f),
            size = Size(blockSize, blockSize),
        )
    }
}

fun DrawScope.drawNoorSecondMarker(
    center: Offset,
    radius: Float,
    second: Int,
    markerColor: Color,
    markerRadius: Float,
) {
    val angle = second * 6f * PI.toFloat() / 180f - PI.toFloat() / 2f
    drawCircle(
        markerColor,
        markerRadius,
        center + Offset(cos(angle) * radius, sin(angle) * radius),
    )
}

fun DrawScope.drawClockHand(
    center: Offset,
    length: Float,
    degrees: Float,
    color: Color,
    width: Float,
) {
    val radians = degrees / 180f * PI
    drawLine(
        color = color,
        start = center,
        end = center + Offset(cos(radians).toFloat() * length, sin(radians).toFloat() * length),
        strokeWidth = width,
        cap = StrokeCap.Round,
    )
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRingHand(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    degrees: Float,
    color: Color,
    width: Float,
) {
    val radians = degrees / 180f * PI
    val cos = cos(radians).toFloat()
    val sin = sin(radians).toFloat()
    val start = center + Offset(cos * innerRadius, sin * innerRadius)
    val end = center + Offset(cos * outerRadius, sin * outerRadius)
    drawLine(color, start, end, width, StrokeCap.Round)
    drawCircle(color, width * 2.2f, end)
}
