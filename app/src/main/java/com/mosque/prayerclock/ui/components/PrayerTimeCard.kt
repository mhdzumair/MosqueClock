package com.mosque.prayerclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.R

@Composable
fun PrayerTimeCard(
    prayerInfo: PrayerInfo,
    isNext: Boolean = false,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 12.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = prayerInfo.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = when {
                        isNext && !isCompact -> 28.sp
                        isNext && isCompact -> 22.sp
                        isCompact -> 18.sp
                        else -> 24.sp
                    },
                    fontWeight = FontWeight.Bold
                ),
                color = if (isNext) 
                    MaterialTheme.colorScheme.onPrimary
                else 
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))
            
            Text(
                text = prayerInfo.azanTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = when {
                        isNext && !isCompact -> 42.sp
                        isNext && isCompact -> 32.sp
                        isCompact -> 28.sp
                        else -> 36.sp
                    },
                    fontWeight = FontWeight.Bold
                ),
                color = if (isNext) 
                    MaterialTheme.colorScheme.onPrimary
                else 
                    MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            prayerInfo.iqamahTime?.let { iqamahTime ->
                Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))
                
                if (isCompact) {
                    Text(
                        text = "${localizedStringResource(R.string.iqamah)}: $iqamahTime",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isNext) 
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${localizedStringResource(R.string.iqamah)}: ",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp
                            ),
                            color = if (isNext) 
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = iqamahTime,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isNext) 
                                MaterialTheme.colorScheme.onPrimary
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}