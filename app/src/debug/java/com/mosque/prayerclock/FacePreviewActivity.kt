package com.mosque.prayerclock

import com.mosque.prayerclock.data.model.Mazhab

import android.os.Bundle
import android.content.pm.ActivityInfo
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.model.PrayerInfo
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
import com.mosque.prayerclock.viewmodel.MainUiState
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
        val face = ScreenFaceRegistry.get(faceId)
        requestedOrientation = if (face.id.endsWith("mobile")) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        val requestedTheme = intent.getStringExtra("theme")
        val themeId =
            if (requestedTheme.isNullOrBlank() || requestedTheme == "recommended") {
                face.recommendedColorThemeId ?: AppColorThemes.ClassicMosque.id
            } else {
                requestedTheme
            }
        val colorTheme = AppColorThemes.getThemeById(themeId)
        val language =
            when (intent.getStringExtra("language")) {
                "en" -> Language.ENGLISH
                "si" -> Language.SINHALA
                else -> Language.TAMIL
            }
        setContent {
            MosqueClockTheme(colorTheme = colorTheme) {
                LocalizedApp(language) {
                    FacePreview(
                        face.id,
                        scenario,
                        language,
                        mazhab = if (intent.getStringExtra("mazhab") == "hanafi") Mazhab.HANAFI else Mazhab.SHAFI,
                        show24Hour = intent.getBooleanExtra("show24Hour", false),
                        showSeconds = intent.getBooleanExtra("showSeconds", true),
                        showWeather = intent.getBooleanExtra("showWeather", true),
                        showAzan = intent.getBooleanExtra("showAzan", true),
                        mosqueName = intent.getStringExtra("mosqueName") ?: "Dickwella Jummah Mosque",
                        clockType = when (intent.getStringExtra("clockType")) {
                            "analog" -> ClockType.ANALOG
                            "digital" -> ClockType.DIGITAL
                            "both" -> ClockType.BOTH
                            else -> if (scenario in listOf("analog", "error-analog")) ClockType.ANALOG else ClockType.DIGITAL
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FacePreview(
    faceId: String,
    scenario: String,
    language: Language,
    mazhab: Mazhab,
    show24Hour: Boolean,
    showSeconds: Boolean,
    showWeather: Boolean,
    showAzan: Boolean,
    mosqueName: String,
    clockType: ClockType,
) {
    val face = ScreenFaceRegistry.get(faceId)
    val hijriMonth = localizedStringArrayResource(R.array.hijri_months)[2]
    val (previewHour, previewMinute, previewSecond) =
        when (scenario) {
            "azan-hour" -> Triple(11, 30, 0)
            "iqamah-hour" -> Triple(12, 20, 0)
            "after-isha" -> Triple(20, 43, 6)
            "countdown" -> Triple(19, 29, 14)
            "iqamah-countdown", "friday-night-bayan" -> Triple(19, 39, 14)
            "sunrise-hour" -> Triple(5, 45, 0)
            "ishraq-hour" -> Triple(6, 20, 0)
            "sunrise-countdown" -> Triple(6, 11, 14)
            "ishraq-countdown" -> Triple(6, 31, 14)
            else -> Triple(12, 36, 25)
        }
    val period = localizedStringResource(if (previewHour < 12) R.string.am else R.string.pm)
    val clock =
        ClockDisplayData.PREVIEW.copy(
            formattedTime = buildString {
                append(if (show24Hour) "%02d".format(previewHour) else "%d".format(if (previewHour % 12 == 0) 12 else previewHour % 12))
                append(":%02d".format(previewMinute))
                if (showSeconds) append(":%02d".format(previewSecond))
                if (!show24Hour) append(" $period")
            },
            hour12 = previewHour % 12,
            hour24 = previewHour,
            minute = previewMinute,
            second = previewSecond,
            isAm = previewHour < 12,
            show24Hour = show24Hour,
            showSeconds = showSeconds,
            hijriDay = 7,
            hijriMonthName = hijriMonth,
            hijriYear = 1448,
            gregorianDay = if (scenario == "friday-night-bayan") 20 else 22,
            gregorianMonthName =
                when (language) {
                    Language.TAMIL -> "ஆகஸ்ட்"
                    Language.SINHALA -> "අගෝස්තු"
                    else -> "August"
                },
            gregorianYear = 2026,
        )
    val base =
        ScreenDisplayData.PREVIEW.copy(
            globalShowAzan = showAzan,
            settings =
                AppSettings(
                    language = language,
                    mazhab = mazhab,
                    mosqueName = mosqueName,
                    clockType = clockType,
                    showSeconds = showSeconds,
                    show24HourFormat = show24Hour,
                    showWeather = showWeather,
                    colorTheme = AppColorThemes.ClassicMosque.id,
                    screenFaceId = face.id,
                ),
            clock = clock,
            mainState = when (scenario) {
                "loading" -> MainUiState.Loading
                "error", "error-analog" -> MainUiState.Error(localizedStringResource(R.string.error_no_data))
                else -> (ScreenDisplayData.PREVIEW.mainState as MainUiState.Success).copy(
                    // Fixed debug fixtures make Shafi/Hanafi screenshot comparisons repeatable.
                    // Production schedules use the selected provider's published Asr times.
                    prayerTimes = (ScreenDisplayData.PREVIEW.mainState as MainUiState.Success).prayerTimes.prayerTimes
                        .copy(asrShafiAzan = "15:30", asrHanafiAzan = "16:30")
                        .withIqamahTimes(AppSettings(mazhab = mazhab)),
                    nextPrayer = when (scenario) {
                        "azan-hour", "iqamah-hour" -> PrayerType.DHUHR
                        "sunrise-hour", "ishraq-hour" -> PrayerType.SUNRISE
                        "after-isha" -> PrayerType.FAJR
                        else -> PrayerType.ASR
                    },
                    nextDayFajr = if (scenario == "after-isha") {
                        val today = (ScreenDisplayData.PREVIEW.mainState as MainUiState.Success).prayerTimes
                        today.copy(
                            prayerTimes = today.prayerTimes.copy(date = "2026-08-23", fajrAzan = "04:40"),
                            fajrIqamah = "04:50",
                        )
                    } else null,
                )
            },
            // Keep the deterministic instant synchronized with the time rendered by the clock.
            currentTime =
                when (scenario) {
                    "azan-hour" -> Instant.parse("2026-08-22T06:00:00Z")
                    "iqamah-hour" -> Instant.parse("2026-08-22T06:50:00Z")
                    "after-isha" -> Instant.parse("2026-08-22T15:13:06Z")
                    "countdown" -> Instant.parse("2026-08-22T13:59:14Z")
                    "iqamah-countdown" -> Instant.parse("2026-08-22T14:09:14Z")
                    "friday-night-bayan" -> Instant.parse("2026-08-20T14:09:14Z")
                    "sunrise-hour" -> Instant.parse("2026-08-22T00:15:00Z")
                    "ishraq-hour" -> Instant.parse("2026-08-22T00:50:00Z")
                    "sunrise-countdown" -> Instant.parse("2026-08-22T00:41:14Z")
                    "ishraq-countdown" -> Instant.parse("2026-08-22T01:01:14Z")
                    else -> Instant.parse("2026-08-22T07:06:25Z")
                },
            inlineCountdownPrayer = if (scenario == "ishraq-hour") {
                PrayerInfo(
                    type = PrayerType.SUNRISE,
                    azanTime = "06:35",
                    iqamahTime = null,
                    name = localizedStringResource(R.string.ishraq),
                )
            } else null,
            isCountdownVisibleForWeight = scenario in listOf("azan-hour", "iqamah-hour", "sunrise-hour", "ishraq-hour"),
            shouldShowFullScreenCountdown = scenario in listOf("countdown", "iqamah-countdown", "friday-night-bayan", "sunrise-countdown", "ishraq-countdown"),
            fullScreenCountdownData =
                if (scenario in listOf("countdown", "iqamah-countdown", "friday-night-bayan", "sunrise-countdown", "ishraq-countdown")) {
                    FullScreenCountdownData(
                        prayerName = localizedStringResource(when (scenario) {
                            "sunrise-countdown" -> R.string.sunrise
                            "ishraq-countdown" -> R.string.ishraq
                            else -> R.string.isha
                        }),
                        prayerType = if (scenario in listOf("sunrise-countdown", "ishraq-countdown")) PrayerType.SUNRISE else PrayerType.ISHA,
                        isIqamah = scenario in listOf("iqamah-countdown", "friday-night-bayan"),
                        minutes = 3,
                        seconds = 46,
                        azanTime = when (scenario) {
                            "sunrise-countdown" -> "06:15"
                            "ishraq-countdown" -> "06:35"
                            else -> "19:33"
                        },
                        targetTime = when (scenario) {
                            "sunrise-countdown" -> "06:15"
                            "ishraq-countdown" -> "06:35"
                            "countdown" -> "19:33"
                            else -> "19:43"
                        },
                        totalDurationSeconds = 10 * 60,
                        kind = when (scenario) {
                            "friday-night-bayan" -> CountdownKind.FRIDAY_NIGHT_BAYAN
                            "iqamah-countdown" -> CountdownKind.IQAMAH
                            "ishraq-countdown" -> CountdownKind.ISHRAQ
                            else -> CountdownKind.AZAN
                        },
                    )
                } else {
                    null
                },
            shouldShowDua = scenario == "dua",
            jummahData = if (scenario == "bayan") JummahData(0, 42, 44) else null,
        )
    face.Render(
        data = base,
        callbacks = ScreenFaceCallbacks(onOpenSettings = {}, onPrayerTransition = {}),
        modifier = Modifier,
    )
}
