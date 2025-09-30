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

@Composable
fun WeatherCard(
    weatherInfo: WeatherInfo,
    modifier: Modifier = Modifier,
) {
    val cardPadding = 6.dp // Balanced padding for proper spacing
    val temperatureFontSize = 32.sp // Readable temperature size
    val detailsFontSize = 20.sp // Readable detail text that fits properly

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
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit,
                    )

                    // Just show temperature - icon is self-explanatory
                    Text(
                        text = "${weatherInfo.temperature}°C",
                        style =
                            MaterialTheme.typography.headlineSmall.copy(
                                fontSize = temperatureFontSize,
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
                            fontSize = detailsFontSize,
                        )

                        WeatherDetailRow(
                            icon = Icons.Filled.WaterDrop,
                            value = "${weatherInfo.humidity}%",
                            fontSize = detailsFontSize,
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
                                fontSize = detailsFontSize,
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
                                fontSize = detailsFontSize,
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

@Composable
private fun WeatherDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
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
            modifier = Modifier.size(22.dp), // Slightly larger to match text size
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
