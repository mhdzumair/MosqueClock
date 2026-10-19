package com.mosque.prayerclock.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mosque.prayerclock.MainActivity
import com.mosque.prayerclock.R
import com.mosque.prayerclock.utils.DeviceUtils
import com.mosque.prayerclock.data.model.Language
import android.content.res.Configuration
import java.util.Locale
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MobileNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun systemBarsRemainVisibleWhenOpeningAndReturningFromSettings() {
        assumeFalse(DeviceUtils.isTelevision(compose.activity))
        val contexts = listOf(Language.ENGLISH, Language.TAMIL, Language.SINHALA).map {
            val configuration = Configuration(compose.activity.resources.configuration)
            configuration.setLocale(Locale(it.code))
            compose.activity.createConfigurationContext(configuration)
        }
        compose.waitUntil(15_000) {
            contexts.any { context ->
                compose.onAllNodes(androidx.compose.ui.test.hasContentDescription(context.getString(R.string.settings)))
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
        val context = contexts.first { context ->
            compose.onAllNodes(androidx.compose.ui.test.hasContentDescription(context.getString(R.string.settings)))
                .fetchSemanticsNodes().isNotEmpty()
        }
        val settings = context.getString(R.string.settings)
        assertSystemBarsVisible()
        capture("home")
        compose.onNodeWithContentDescription(settings).performClick()
        capture("settings-prayers")
        val display = context.getString(R.string.settings_section_display)
        compose.onNodeWithText(display).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.weather)).assertIsDisplayed()
        capture("settings-display")
        assertSystemBarsVisible()
        compose.onNodeWithText(context.getString(R.string.settings_section_alerts)).performScrollTo().performClick()
        capture("settings-reminders")
        compose.onNodeWithText(context.getString(R.string.settings_section_app)).performScrollTo().performClick()
        capture("settings-app")
        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        compose.onNodeWithContentDescription(settings).assertIsDisplayed()
        assertSystemBarsVisible()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(350) // Allow the device to present the completed transition.
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "mobile-verification")
        directory.mkdirs()
        instrumentation.uiAutomation.takeScreenshot().useBitmap { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun Bitmap.useBitmap(block: (Bitmap) -> Unit) {
        try { block(this) } finally { recycle() }
    }

    private fun assertSystemBarsVisible() {
        compose.runOnIdle {
            val insets = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)!!
            assertTrue("Status bar must remain visible", insets.isVisible(WindowInsetsCompat.Type.statusBars()))
            assertTrue("Navigation bar must remain visible", insets.isVisible(WindowInsetsCompat.Type.navigationBars()))
        }
    }
}
