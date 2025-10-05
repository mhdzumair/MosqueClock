package com.mosque.prayerclock.ui.components

import android.text.util.Linkify
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText

/**
 * Composable that renders markdown text using the compose-markdown library.
 *
 * @param markdown The markdown text to render
 * @param modifier Modifier for the markdown text
 * @param color Text color
 * @param style Text style
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodySmall,
) {
    MarkdownText(
        markdown = markdown,
        modifier = modifier.fillMaxWidth(),
        color = color,
        style = style,
        fontSize = style.fontSize.takeIf { it != 0.sp } ?: 14.sp,
        fontResource = null, // Use default system font
        disableLinkMovementMethod = false, // Enable link clicking
        linkifyMask = Linkify.ALL, // Linkify all types of links
    )
}
