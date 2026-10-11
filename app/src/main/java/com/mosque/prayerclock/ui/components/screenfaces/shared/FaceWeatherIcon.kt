package com.mosque.prayerclock.ui.components.screenfaces.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun FaceWeatherIcon(
    header: FaceHeaderData,
    fallbackColor: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val iconUrl =
        header.weatherIcon?.let {
            when {
                it.startsWith("http") -> it
                it.startsWith("//") -> "https:$it"
                else -> null
            }
        }
    if (iconUrl != null) {
        val context = LocalContext.current
        val request = remember(context, iconUrl) {
            ImageRequest.Builder(context).data(iconUrl).crossfade(true).build()
        }
        AsyncImage(
            model = request,
            contentDescription = header.condition,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    } else {
        val glyph = weatherGlyph(header.weatherIcon)
        if (glyph.isNotBlank()) {
            val fontSize = with(LocalDensity.current) { (size * 0.72f).toSp() }
            Box(modifier.size(size), contentAlignment = Alignment.Center) {
                Text(
                    text = glyph,
                    color = fallbackColor,
                    fontSize = fontSize,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
