package com.mosque.prayerclock.ui

import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

@Composable
@ReadOnlyComposable
fun localizedStringResource(
    @StringRes id: Int,
): String = LocalLocalizedContext.current.getString(id)

@Composable
@ReadOnlyComposable
fun localizedStringResource(
    @StringRes id: Int,
    vararg formatArgs: Any,
): String = LocalLocalizedContext.current.getString(id, *formatArgs)

@Composable
@ReadOnlyComposable
fun localizedStringArrayResource(
    @StringRes id: Int,
): Array<String> = LocalLocalizedContext.current.resources.getStringArray(id)

// Animated version for smooth language transitions
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedLocalizedText(
    @StringRes stringResId: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    enableAnimation: Boolean = true, // Allow disabling animation for performance
) {
    val text = localizedStringResource(stringResId)

    if (enableAnimation) {
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(100, easing = LinearEasing), // Ultra-fast for minimal interference
                ) togetherWith
                    fadeOut(
                        animationSpec = tween(50, easing = LinearEasing),
                    )
            },
            label = "localized_text_transition",
        ) { animatedText ->
            Text(
                text = animatedText,
                modifier = modifier,
                style = style,
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = textAlign,
                maxLines = maxLines,
                softWrap = softWrap,
            )
        }
    } else {
        // No animation - direct text for maximum performance
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
            softWrap = softWrap,
        )
    }
}
