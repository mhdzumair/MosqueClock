package com.mosque.prayerclock.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.model.WeatherInfo
import com.mosque.prayerclock.ui.AnimatedLocalizedText
import com.mosque.prayerclock.ui.localizedStringResource
import kotlin.math.min

@Composable
fun WeatherCard(
    weatherInfo: WeatherInfo,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxSize() // Remove fixed height to allow auto-scaling
                .padding(2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxSize().padding(3.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(9.dp),
        ) {
            // Use BoxWithConstraints for dynamic sizing
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                val density = LocalDensity.current
                val availableHeightPx = with(density) { maxHeight.toPx() }
                val availableWidthPx = with(density) { maxWidth.toPx() }

                // Calculate dynamic sizes based on available space
                val calculatedTemperatureFontSize =
                    with(density) {
                        // Temperature should use about 25% of height
                        val heightBasedSize = (availableHeightPx * 0.25f).toSp()

                        // Consider width (typical: "28.8°C" = 6 chars)
                        val widthBasedSize = (availableWidthPx / 10f * 1.5f).toSp()

                        min(heightBasedSize.value, widthBasedSize.value).sp
                    }

                // Details font is 60% of temperature size
                val calculatedDetailsFontSize = calculatedTemperatureFontSize * 0.60f

                // Icon size is proportional to temperature font
                val calculatedIconSize =
                    with(density) {
                        (calculatedTemperatureFontSize.toPx() * 1.3f).toDp()
                    }

                // Detail icon size is proportional to details font
                val calculatedDetailIconSize =
                    with(density) {
                        (calculatedDetailsFontSize.toPx() * 1.1f).toDp()
                    }

                val cardPadding = 6.dp // Balanced padding for proper spacing

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(cardPadding),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Top row: weather icon + temperature
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing for larger elements
                    ) {
                        // Log weather info only once when it changes, not on every recomposition
                        LaunchedEffect(
                            weatherInfo.icon,
                            weatherInfo.temperature,
                            weatherInfo.uvIndex,
                            weatherInfo.windSpeed,
                        ) {
                            Log.d(
                                "WeatherCard",
                                "Weather info updated - Icon: '${weatherInfo.icon}', Temperature: ${weatherInfo.temperature} -> ${weatherInfo.temperature.toInt()}°C, UV: ${weatherInfo.uvIndex} -> UV ${weatherInfo.uvIndex?.toInt()}, Wind: ${weatherInfo.windSpeed} -> ${weatherInfo.windSpeed?.toInt()}km/h, FeelsLike: ${weatherInfo.feelsLike} -> ${weatherInfo.feelsLike.toInt()}°",
                            )
                        }

                        // Direct weather icon from API
                        val iconUrl =
                            if (weatherInfo.icon.startsWith("http")) {
                                weatherInfo.icon
                            } else {
                                "https:${weatherInfo.icon}"
                            }

                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalContext.current)
                                    .data(iconUrl)
                                    .crossfade(true)
                                    .build(),
                            contentDescription = weatherInfo.description,
                            modifier =
                                Modifier
                                    .size(calculatedIconSize)
                                    .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
                        )

                        // Just show temperature - icon is self-explanatory
                        Text(
                            text = "${weatherInfo.temperature}°C",
                            style =
                                MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = calculatedTemperatureFontSize,
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // Bottom: Weather details in a 2x2 grid for better space utilization
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Row 1: Feels like and Humidity
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            WeatherDetailRow(
                                icon = Icons.Filled.Thermostat,
                                value = "${weatherInfo.feelsLike}°",
                                fontSize = calculatedDetailsFontSize,
                                iconSize = calculatedDetailIconSize,
                            )

                            WeatherDetailRow(
                                icon = Icons.Filled.WaterDrop,
                                value = "${weatherInfo.humidity}%",
                                fontSize = calculatedDetailsFontSize,
                                iconSize = calculatedDetailIconSize,
                            )
                        }

                        // Row 2: Wind speed and UV Index
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Wind speed (if available, otherwise show placeholder)
                            weatherInfo.windSpeed?.let { wind ->
                                WeatherDetailRow(
                                    icon = Icons.Filled.Air,
                                    value = "${wind}km/h",
                                    fontSize = calculatedDetailsFontSize,
                                    iconSize = calculatedDetailIconSize,
                                )
                            } ?: run {
                                // Placeholder to maintain layout balance
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            // UV Index (if available, otherwise show placeholder)
                            weatherInfo.uvIndex?.let { uv ->
                                WeatherDetailRow(
                                    icon = Icons.Filled.WbSunny,
                                    value = "UV $uv",
                                    fontSize = calculatedDetailsFontSize,
                                    iconSize = calculatedDetailIconSize,
                                )
                            } ?: run {
                                // Placeholder to maintain layout balance
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    iconSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Text(
            text = value,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium, // Added medium weight for better readability
                ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), // Increased alpha from 0.7f to 0.8f
        )
    }
}
