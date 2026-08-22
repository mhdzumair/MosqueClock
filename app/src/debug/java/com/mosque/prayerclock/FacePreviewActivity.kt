package com.mosque.prayerclock

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.LocalizedApp
import com.mosque.prayerclock.ui.components.clockfaces.ClockDisplayData
import com.mosque.prayerclock.ui.components.screenfaces.CountdownKind
import com.mosque.prayerclock.ui.components.screenfaces.FullScreenCountdownData
import com.mosque.prayerclock.ui.components.screenfaces.HorizonScreenFace
import com.mosque.prayerclock.ui.components.screenfaces.JummahData
import com.mosque.prayerclock.ui.components.screenfaces.Render
import com.mosque.prayerclock.ui.components.screenfaces.ScreenDisplayData
import com.mosque.prayerclock.ui.components.screenfaces.ScreenFaceCallbacks
import com.mosque.prayerclock.ui.components.screenfaces.ScreenFaceRegistry
import com.mosque.prayerclock.ui.localizedStringArrayResource
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.AppColorThemes
import com.mosque.prayerclock.ui.theme.MosqueClockTheme
import kotlinx.datetime.Instant

/** Debug-only deterministic host used by scripts/capture_face_scenarios.sh. */
class FacePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        // A single software-rendered layer makes emulator screencaps deterministic; production
        // activities keep hardware acceleration enabled.
        window.decorView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        val scenario = intent.getStringExtra("scenario") ?: "digital"
        val faceId = intent.getStringExtra("face") ?: HorizonScreenFace.id
        val colorTheme = AppColorThemes.getThemeById(
            intent.getStringExtra("theme") ?: AppColorThemes.ClassicMosque.id,
        )
        val language = when (intent.getStringExtra("language")) {
            "en" -> Language.ENGLISH
            "si" -> Language.SINHALA
            else -> Language.TAMIL
        }
        setContent {
            MosqueClockTheme(colorTheme = colorTheme) {
                LocalizedApp(language) {
                    FacePreview(faceId, scenario, language)
                }
            }
        }
    }
}

@Composable
private fun FacePreview(faceId: String, scenario: String, language: Language) {
    val face = ScreenFaceRegistry.get(faceId)
    val isAnalog = scenario == "analog"
    val period = localizedStringResource(R.string.pm)
    val hijriMonth = localizedStringArrayResource(R.array.hijri_months)[2]
    val clock = ClockDisplayData.PREVIEW.copy(
        formattedTime = "12:36:25 $period",
        hour12 = 0,
        hour24 = 12,
        minute = 36,
        second = 25,
        showSeconds = true,
        hijriDay = 7,
        hijriMonthName = hijriMonth,
        hijriYear = 1448,
        gregorianDay = 21,
        gregorianMonthName = when (language) {
            Language.TAMIL -> "ஆகஸ்ட்"
            Language.SINHALA -> "අගෝස්තු"
            else -> "August"
        },
        gregorianYear = 2026,
    )
    val base = ScreenDisplayData.PREVIEW.copy(
        settings = AppSettings(
            language = language,
            mosqueName = "Dickwella Jummah Mosque",
            clockType = if (isAnalog) ClockType.ANALOG else ClockType.DIGITAL,
            showSeconds = true,
            show24HourFormat = false,
            showWeather = true,
            colorTheme = AppColorThemes.ClassicMosque.id,
            screenFaceId = face.id,
        ),
        clock = clock,
        // 11:30 in Asia/Colombo: Dhuhr azan at 12:15 is exactly 45 minutes away.
        currentTime = Instant.parse("2026-08-22T06:00:00Z"),
        isCountdownVisibleForWeight = scenario == "azan-hour",
        shouldShowFullScreenCountdown = scenario == "countdown",
        fullScreenCountdownData = if (scenario == "countdown") {
            FullScreenCountdownData(
                prayerName = localizedStringResource(R.string.isha),
                prayerType = PrayerType.ISHA,
                isIqamah = false,
                minutes = 3,
                seconds = 46,
                azanTime = "19:33",
                kind = CountdownKind.AZAN,
            )
        } else null,
        shouldShowDua = scenario == "dua",
        jummahData = if (scenario == "bayan") JummahData(0, 42, 44) else null,
    )
    face.Render(
        data = base,
        callbacks = ScreenFaceCallbacks(onOpenSettings = {}, onPrayerTransition = {}),
        modifier = Modifier,
    )
}
