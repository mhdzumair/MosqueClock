package com.mosque.prayerclock.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.model.WeatherInfo
import com.mosque.prayerclock.R

@Composable
fun WeatherCard(
    weatherInfo: WeatherInfo,
    language: Language = Language.ENGLISH,
    isTV: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardPadding = if (isTV) 12.dp else 10.dp
    val temperatureFontSize = if (isTV) 28.sp else 22.sp  // Increased temperature font size
    val detailsFontSize = if (isTV) 14.sp else 12.sp      // Increased details font size
    
    Card(
        modifier = modifier
            .height(if (isTV) 60.dp else 50.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Temperature and description
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "${weatherInfo.temperature.toInt()}°C",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = temperatureFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = weatherInfo.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = detailsFontSize
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
            
            // Compact details
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${localizedStringResource(R.string.humidity_short)}: ${weatherInfo.humidity}%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = detailsFontSize
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "${localizedStringResource(R.string.feels_like_short)}: ${weatherInfo.feelsLike.toInt()}°",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = detailsFontSize
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}