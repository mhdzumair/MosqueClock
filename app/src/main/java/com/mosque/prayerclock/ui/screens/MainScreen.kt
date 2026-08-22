package com.mosque.prayerclock.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerTimesWithIqamah
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.components.PrayerTimeCard
import com.mosque.prayerclock.ui.components.screenfaces.ScreenFaceCallbacks
import com.mosque.prayerclock.ui.components.screenfaces.ScreenFaceRegistry
import com.mosque.prayerclock.ui.components.screenfaces.Render
import com.mosque.prayerclock.ui.components.screenfaces.rememberScreenDisplayData
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.utils.NetworkMonitor
import com.mosque.prayerclock.utils.SystemChangeMonitor
import com.mosque.prayerclock.viewmodel.MainViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun MainScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    // Monitor prayer-related settings changes — drives viewModel.loadPrayerTimes()
    val data = rememberScreenDisplayData(viewModel)

    LaunchedEffect(
        data.settings.prayerServiceType,
        data.settings.city,
        data.settings.country,
        data.settings.showWeather,
    ) {
        viewModel.loadPrayerTimes()
    }

    LaunchedEffect(Unit) {
        SystemChangeMonitor.observeSystemChanges(context).collect { event ->
            Log.d("MainScreen", "System change event: $event")
            viewModel.onSystemChangeEvent(event)
        }
    }

    LaunchedEffect(Unit) {
        NetworkMonitor.observeNetworkStatus(context).collect { status ->
            Log.d("MainScreen", "Network status: $status")
            viewModel.onNetworkStatusChange(status)
        }
    }

    val face = ScreenFaceRegistry.get(data.settings.screenFaceId)
    val callbacks = ScreenFaceCallbacks(
        onOpenSettings = onOpenSettings,
        onPrayerTransition = { viewModel.updateNextPrayer() },
    )
    face.Render(data, callbacks, Modifier)
}

@Composable
fun AnimatedPrayerTimesSection(
    prayerTimes: PrayerTimesWithIqamah,
    nextPrayer: PrayerType?,
    show24Hour: Boolean = false,
    currentTime: Instant = Clock.System.now(),
    globalShowAzan: Boolean = true,
) {
    val prayerInfoList = createPrayerInfoList(prayerTimes, currentTime)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 0.5.dp),
        horizontalArrangement = Arrangement.spacedBy(0.5.dp),
    ) {
        prayerInfoList.forEach { prayerInfo ->
            PrayerTimeCard(
                prayerInfo = prayerInfo,
                isNext = prayerInfo.type == nextPrayer,
                show24Hour = show24Hour,
                globalShowAzan = globalShowAzan,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun createPrayerInfoList(
    prayerTimes: PrayerTimesWithIqamah,
    currentTime: Instant = Clock.System.now(),
): List<PrayerInfo> {
    val currentDate = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val isFriday = currentDate.dayOfWeek == DayOfWeek.FRIDAY

    return listOf(
        PrayerInfo(
            type = PrayerType.FAJR,
            azanTime = prayerTimes.fajrAzan,
            iqamahTime = prayerTimes.fajrIqamah,
            name = localizedStringResource(R.string.fajr),
        ),
        PrayerInfo(
            type = PrayerType.SUNRISE,
            azanTime = prayerTimes.sunrise,
            iqamahTime = null,
            name = localizedStringResource(R.string.sunrise),
        ),
        PrayerInfo(
            type = PrayerType.DHUHR,
            azanTime = prayerTimes.dhuhrAzan,
            iqamahTime = if (isFriday) null else prayerTimes.dhuhrIqamah,
            name = if (isFriday) localizedStringResource(R.string.jummah) else localizedStringResource(R.string.dhuhr),
        ),
        PrayerInfo(
            type = PrayerType.ASR,
            azanTime = prayerTimes.asrAzan,
            iqamahTime = prayerTimes.asrIqamah,
            name = localizedStringResource(R.string.asr),
        ),
        PrayerInfo(
            type = PrayerType.MAGHRIB,
            azanTime = prayerTimes.maghribAzan,
            iqamahTime = prayerTimes.maghribIqamah,
            name = localizedStringResource(R.string.maghrib),
        ),
        PrayerInfo(
            type = PrayerType.ISHA,
            azanTime = prayerTimes.ishaAzan,
            iqamahTime = prayerTimes.ishaIqamah,
            name = localizedStringResource(R.string.isha),
        ),
    )
}
