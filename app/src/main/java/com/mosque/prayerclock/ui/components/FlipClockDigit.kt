package com.mosque.prayerclock.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared flip clock digit pair component - reusable across MainScreen and FullScreenCountdown
 */
@Composable
fun FlipClockDigitPair(
    value: Long,
    digitBoxSize: TextUnit = 64.sp,
) {
    val tens = (value / 10).toInt()
    val units = (value % 10).toInt()

    // Calculate spacing proportional to digit size
    val pairSpacing = with(LocalDensity.current) { (digitBoxSize.toPx() * 0.06f).toDp() }

    Row(
        horizontalArrangement = Arrangement.spacedBy(pairSpacing),
    ) {
        AnimatedFlipDigit(digit = tens, digitBoxSize = digitBoxSize)
        AnimatedFlipDigit(digit = units, digitBoxSize = digitBoxSize)
    }
}

/**
 * Animated flip digit with forest green cards and brass accents
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedFlipDigit(
    digit: Int,
    digitBoxSize: TextUnit = 64.sp,
) {
    var previousDigit by remember { mutableStateOf(digit) }
    var currentDigit by remember { mutableStateOf(digit) }

    // Detect digit change for animation
    LaunchedEffect(digit) {
        if (digit != currentDigit) {
            previousDigit = currentDigit
            currentDigit = digit
        }
    }

    val density = LocalDensity.current

    // Calculate box dimensions based on digitBoxSize
    // Box should be proportional: width is ~0.73x of height
    val boxHeight = with(density) { digitBoxSize.toDp() * 1.38f }
    val boxWidth = boxHeight * 0.73f

    // Font size is ~64% of box height
    val fontSize = digitBoxSize

    // Border radius proportional to box size
    val borderRadius = boxHeight * 0.09f

    Box(
        modifier = Modifier.size(width = boxWidth, height = boxHeight),
    ) {
        // Main flip card background - elegant natural color matching your mosque clock theme
        Card(
            modifier = Modifier.fillMaxSize(),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color(0xFF2D4A22), // Deep forest green matching your analog clock
                ),
            shape = RoundedCornerShape(borderRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            // Inner card with elegant brass-tinted background
            Card(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                // Thin border to show outer color
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color(0xFF3A5F2A), // Slightly lighter forest green
                    ),
                shape = RoundedCornerShape(borderRadius * 0.75f),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    // Animated digit transition
                    AnimatedContent(
                        targetState = currentDigit,
                        transitionSpec = {
                            if (targetState > initialState) {
                                // Flip up animation
                                slideInVertically { height -> -height } + fadeIn() togetherWith
                                    slideOutVertically { height -> height } + fadeOut()
                            } else {
                                // Flip down animation
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                    slideOutVertically { height -> -height } + fadeOut()
                            }.using(
                                SizeTransform(clip = false),
                            )
                        },
                        label = "digit_flip",
                    ) { animatedDigit ->
                        Text(
                            text = animatedDigit.toString(),
                            style =
                                MaterialTheme.typography.displayLarge.copy(
                                    fontSize = fontSize,
                                    fontWeight = FontWeight.Black, // Extra bold for visibility
                                ),
                            color = Color(0xFFB08D57), // Elegant brass/gold color matching your analog clock
                        )
                    }

                    // Horizontal line in the middle to simulate flip mechanism - subtle brass accent
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFB08D57).copy(alpha = 0.6f)) // Subtle brass line
                                .align(Alignment.Center),
                    )

                    // Add subtle gradient shadows for depth like your analog clock
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Black.copy(alpha = 0.2f),
                                                Color.Transparent,
                                            ),
                                    ),
                                ).align(Alignment.TopCenter),
                    )

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.2f),
                                            ),
                                    ),
                                ).align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}
