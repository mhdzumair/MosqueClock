package com.mosque.prayerclock.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.ui.localizedStringResource

@Composable
fun PrayerTimeCard(
    prayerInfo: PrayerInfo,
    isNext: Boolean = false,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    Card(
        modifier =
            modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp))
                .let { cardModifier ->
                    if (isNext) {
                        cardModifier.border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp),
                        )
                    } else {
                        cardModifier
                    }
                },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isNext) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        },
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(if (isCompact) 12.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = prayerInfo.name,
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize =
                                when {
                                    isNext && !isCompact -> 28.sp
                                    isNext && isCompact -> 22.sp
                                    isCompact -> 18.sp
                                    else -> 24.sp
                                },
                            fontWeight = FontWeight.Bold,
                        ),
                    color =
                        if (isNext) {
                            // Emphasize with primary color to focus on timing
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))

                Text(
                    text = prayerInfo.azanTime,
                    style =
                        MaterialTheme.typography.displayLarge.copy(
                            fontSize =
                                when {
                                    isNext && !isCompact -> 42.sp
                                    isNext && isCompact -> 32.sp
                                    isCompact -> 28.sp
                                    else -> 36.sp
                                },
                            fontWeight = FontWeight.Bold,
                        ),
                    color =
                        if (isNext) {
                            // Bold primary color to emphasize timing values
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    textAlign = TextAlign.Center,
                )

                prayerInfo.iqamahTime?.let { iqamahTime ->
                    Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))

                    if (isCompact) {
                        Text(
                            text = "${localizedStringResource(R.string.iqamah)}: $iqamahTime",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            color =
                                if (isNext) {
                                    // Emphasize with primary color
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                },
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "${localizedStringResource(R.string.iqamah)}: ",
                                style =
                                    MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                    ),
                                color =
                                    if (isNext) {
                                        // Emphasize with primary color
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    },
                            )
                            Text(
                                text = iqamahTime,
                                style =
                                    MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                color =
                                    if (isNext) {
                                        // Bold primary color to emphasize timing values
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
