package com.mosque.prayerclock.ui.components.screenfaces

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mosque.prayerclock.ui.components.DuaForJoiningSaff
import com.mosque.prayerclock.ui.components.FullScreenCountdown
import com.mosque.prayerclock.ui.components.JummahInProgress

/** A complete visual family for every state shown by the mosque display. */
interface ScreenFace {
    val id: String
    val name: String
    val description: String
    val recommendedColorThemeId: String? get() = null
    val legacyIds: Set<String> get() = emptySet()

    @Composable
    fun Dashboard(
        data: ScreenDisplayData,
        callbacks: ScreenFaceCallbacks,
        modifier: Modifier,
    )

    @Composable
    fun Countdown(data: ScreenDisplayData, countdown: FullScreenCountdownData, modifier: Modifier) {
        FullScreenCountdown(
            prayerName = countdown.prayerName,
            prayerType = countdown.prayerType,
            isIqamah = countdown.isIqamah,
            minutes = countdown.minutes,
            seconds = countdown.seconds,
            azanTime = countdown.azanTime,
            show24Hour = data.settings.show24HourFormat,
            modifier = modifier,
        )
    }

    /** Thursday-night Isha countdown when the configured Friday bayan delay is active. */
    @Composable
    fun FridayNightBayanCountdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) {
        Countdown(data, countdown, modifier)
    }

    @Composable
    fun Dua(data: ScreenDisplayData, modifier: Modifier) {
        DuaForJoiningSaff(modifier)
    }

    @Composable
    fun FridayBayan(data: ScreenDisplayData, bayan: JummahData, modifier: Modifier) {
        JummahInProgress(
            hoursRemaining = bayan.hoursRemaining,
            minutesRemaining = bayan.minutesRemaining,
            secondsRemaining = bayan.secondsRemaining,
            modifier = modifier,
        )
    }
}

/** Owns scene priority once, so implementations cannot drift apart. */
@Composable
fun ScreenFace.Render(
    data: ScreenDisplayData,
    callbacks: ScreenFaceCallbacks,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Dashboard(data, callbacks, Modifier.fillMaxSize())
        data.fullScreenCountdownData
            ?.takeIf { data.shouldShowFullScreenCountdown }
            ?.let {
                if (it.kind == CountdownKind.FRIDAY_NIGHT_BAYAN) {
                    FridayNightBayanCountdown(data, it, Modifier.fillMaxSize())
                } else {
                    Countdown(data, it, Modifier.fillMaxSize())
                }
            }
        if (data.shouldShowDua) Dua(data, Modifier.fillMaxSize())
        data.jummahData?.let { FridayBayan(data, it, Modifier.fillMaxSize()) }
    }
}

object ScreenFaceRegistry {
    val faces: List<ScreenFace> = listOf(
        ClassicScreenFace,
        HorizonScreenFace,
        UfuqScreenFace,
        SafaaScreenFace,
        NoorScreenFace,
        SaharScreenFace,
        KaashScreenFace,
        MihrabScreenFace,
        MobileScreenFace,
        HorizonMobileScreenFace,
    )

    fun get(id: String): ScreenFace = faces.firstOrNull { it.id == id || id in it.legacyIds }
        ?: ClassicScreenFace
}
