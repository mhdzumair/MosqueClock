package com.mosque.prayerclock.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.ui.components.screenfaces.ScreenDisplayData
import com.mosque.prayerclock.ui.components.screenfaces.ScreenFaceCallbacks
import com.mosque.prayerclock.ui.components.screenfaces.ScreenFaceRegistry
import com.mosque.prayerclock.ui.theme.AppColorThemes
import com.mosque.prayerclock.ui.theme.MosqueClockTheme
import com.mosque.prayerclock.viewmodel.MainUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrayerDataUnavailableTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun modernFacesKeepClockTickingAndSettingsAccessibleWithoutPrayerData() {
        var faceId by mutableStateOf("ufuq")
        var data by mutableStateOf(ScreenDisplayData.PREVIEW.copy(mainState = MainUiState.Loading))
        var settingsOpened = 0
        compose.setContent {
            val face = ScreenFaceRegistry.get(faceId)
            MosqueClockTheme(AppColorThemes.getThemeById(face.recommendedColorThemeId ?: "classic_mosque")) {
                LocalizedApp(Language.ENGLISH) {
                    face.Dashboard(data, ScreenFaceCallbacks({ settingsOpened++ }, {}), Modifier.fillMaxSize())
                }
            }
        }
        for (id in listOf("ufuq", "safaa", "noor", "sahar", "kaash", "mihrab")) {
            for (clockType in listOf(ClockType.DIGITAL, ClockType.ANALOG)) {
                for (state in listOf(MainUiState.Loading, MainUiState.Error("Fetch failed"))) {
                    compose.runOnIdle {
                        faceId = id
                        data = ScreenDisplayData.PREVIEW.copy(
                            settings = ScreenDisplayData.PREVIEW.settings.copy(clockType = clockType),
                            mainState = state,
                            errorMessage = null,
                        )
                    }
                    compose.onNodeWithContentDescription("Current Time 10:10").assertIsDisplayed()
                    compose.onNodeWithText(if (state is MainUiState.Error) "Fetch failed" else "Loading…")
                        .assertIsDisplayed()
                    val previous = settingsOpened
                    compose.onNodeWithText("Settings").assertIsDisplayed().performClick()
                    compose.runOnIdle { assertEquals(previous + 1, settingsOpened) }
                    compose.runOnIdle {
                        data = data.copy(clock = data.clock.copy(formattedTime = "10:11", minute = 11))
                    }
                    compose.onNodeWithContentDescription("Current Time 10:11").assertIsDisplayed()
                    // The clock itself must also remain a route to Settings.
                    compose.onNodeWithContentDescription("Current Time 10:11").performClick()
                    compose.runOnIdle { assertEquals(previous + 2, settingsOpened) }
                }
            }
        }
    }
}
