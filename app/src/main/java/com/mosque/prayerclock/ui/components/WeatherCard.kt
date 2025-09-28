package com.mosque.prayerclock.ui.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val cardPadding = 12.dp
    val temperatureFontSize = 28.sp
    val detailsFontSize = 11.sp

    Card(
        modifier =
            modifier
                .height(75.dp)
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
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(cardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left: weather icon + temperature + description
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Log weather info only once when it changes, not on every recomposition
                    LaunchedEffect(weatherInfo.icon, weatherInfo.temperature, weatherInfo.uvIndex, weatherInfo.windSpeed) {
                        Log.d(
                            "WeatherCard",
                            "Weather info updated - Icon: '${weatherInfo.icon}', Temperature: ${weatherInfo.temperature} -> ${weatherInfo.temperature.toInt()}°C, UV: ${weatherInfo.uvIndex} -> UV ${weatherInfo.uvIndex?.toInt()}, Wind: ${weatherInfo.windSpeed} -> ${weatherInfo.windSpeed?.toInt()}km/h, FeelsLike: ${weatherInfo.feelsLike} -> ${weatherInfo.feelsLike.toInt()}°",
                        )
                    }

                    // Direct weather icon from API
                    val iconUrl = if (weatherInfo.icon.startsWith("http")) {
                        weatherInfo.icon
                    } else {
                        "https:${weatherInfo.icon}"
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(iconUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = weatherInfo.description,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
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

                // Right: Weather details with icons in a compact grid
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Row 1: Feels like and Humidity
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WeatherDetailRow(
                            icon = Icons.Filled.Thermostat,
                            value = "${weatherInfo.feelsLike}°",
                            fontSize = detailsFontSize
                        )
                        WeatherDetailRow(
                            icon = Icons.Filled.WaterDrop,
                            value = "${weatherInfo.humidity}%",
                            fontSize = detailsFontSize
                        )
                    }
                    
                    // Row 2: Wind speed and UV Index
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Wind speed with air icon (if available)
                        weatherInfo.windSpeed?.let { wind ->
                            WeatherDetailRow(
                                icon = Icons.Filled.Air,
                                value = "${wind}km/h",
                                fontSize = detailsFontSize
                            )
                        }
                        
                        // UV Index with sun icon (if available)
                        weatherInfo.uvIndex?.let { uv ->
                            WeatherDetailRow(
                                icon = Icons.Filled.WbSunny,
                                value = "UV ${uv}",
                                fontSize = detailsFontSize
                            )
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
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = fontSize),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}
