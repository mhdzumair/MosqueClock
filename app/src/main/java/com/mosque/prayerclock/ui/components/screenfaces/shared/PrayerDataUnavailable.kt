package com.mosque.prayerclock.ui.components.screenfaces.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.ui.components.AnalogClock
import com.mosque.prayerclock.ui.components.TvAutoSizeText
import com.mosque.prayerclock.ui.components.rememberTvTextScale
import com.mosque.prayerclock.ui.components.screenfaces.ScreenDisplayData
import com.mosque.prayerclock.ui.components.screenfaces.ScreenFaceCallbacks
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.LocalColorTheme
import com.mosque.prayerclock.viewmodel.MainUiState

/** Prayer fetch state must never prevent using the clock or recovering through Settings. */
@Composable
fun PrayerDataUnavailable(
    data: ScreenDisplayData,
    callbacks: ScreenFaceCallbacks,
    modifier: Modifier = Modifier,
) {
    val colors = LocalColorTheme.current
    val clockDescription = "${localizedStringResource(R.string.current_time)} ${data.clock.formattedTime}"
    val clockType = when (data.settings.clockType) {
        ClockType.BOTH -> if ((data.clock.minute / 2) % 2 == 0) ClockType.DIGITAL else ClockType.ANALOG
        else -> data.settings.clockType
    }
    val message = when (val state = data.mainState) {
        is MainUiState.Error -> data.errorMessage ?: state.message
        else -> localizedStringResource(R.string.loading)
    }
    BoxWithConstraints(modifier.padding(16.dp)) {
        val scale = rememberTvTextScale(maxWidth, maxHeight)
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .semantics { contentDescription = clockDescription }
                    .clickable(onClick = callbacks.onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                if (clockType == ClockType.ANALOG) {
                    AnalogClock(data.clock, Modifier.fillMaxSize().padding(8.dp))
                } else {
                    FacePanelClock(
                        clock = data.clock,
                        primaryColor = colors.textPrimary,
                        mutedColor = colors.textSecondary,
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        amLabel = localizedStringResource(R.string.am),
                        pmLabel = localizedStringResource(R.string.pm),
                    )
                }
            }
            TvAutoSizeText(
                text = message,
                color = colors.textSecondary,
                maxFontSize = scale.height(.06f),
                minFontSize = scale.height(.035f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
            Button(
                onClick = callbacks.onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.textPrimary,
                    contentColor = colors.backgroundMain,
                ),
            ) {
                Text(localizedStringResource(R.string.settings))
            }
        }
    }
}
