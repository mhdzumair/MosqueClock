package com.mosque.prayerclock.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.mosque.prayerclock.data.cache.PrayerTimesCacheInvalidator
import com.mosque.prayerclock.ui.components.screenfaces.ScreenFaceRegistry
import java.nio.file.Files
import javax.inject.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryTest {
    @Test
    fun `every face and its palette survive storage reopening`() = runBlocking {
        val directory = Files.createTempDirectory("clock-settings").toFile()
        val file = directory.resolve("settings.preferences_pb")
        var job = Job()
        val invalidator = Provider<PrayerTimesCacheInvalidator> { error("Face changes must not invalidate prayer times") }
        fun repository() = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file },
            invalidator,
        )
        try {
            var repository = repository()
            for (face in ScreenFaceRegistry.faces) {
                repository.updateScreenFaceId(face.id, face.recommendedColorThemeId)
                face.recommendedColorThemeId?.let {
                    assertEquals(it, repository.getSettings().first().colorTheme)
                }
                repository.updateColorTheme("custom-palette")
                // Tapping the selected face must preserve a separately chosen palette.
                repository.updateScreenFaceId(face.id, face.recommendedColorThemeId)
                job.cancelAndJoin()
                job = Job()
                repository = repository()
                val saved = repository.getSettings().first()
                assertEquals(face.id, saved.screenFaceId)
                assertEquals("custom-palette", saved.colorTheme)
                assertEquals(face.id, ScreenFaceRegistry.get(saved.screenFaceId).id)
            }
        } finally {
            job.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}
