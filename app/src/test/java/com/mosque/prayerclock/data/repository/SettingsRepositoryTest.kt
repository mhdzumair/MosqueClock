package com.mosque.prayerclock.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.mosque.prayerclock.data.cache.PrayerTimesCacheInvalidator
import com.mosque.prayerclock.data.model.Mazhab
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
    fun `mazhab defaults to Shafi persists both choices and invalidates prayer cache`() = runBlocking {
        val directory = Files.createTempDirectory("clock-mazhab").toFile()
        val file = directory.resolve("settings.preferences_pb")
        var job = Job()
        var invalidations = 0
        val invalidator = object : PrayerTimesCacheInvalidator {
            override fun invalidatePrayerTimesCache() { invalidations++ }
        }
        fun repository() = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file },
            Provider<PrayerTimesCacheInvalidator> { invalidator },
        )
        try {
            var repository = repository()
            assertEquals(Mazhab.SHAFI, repository.getSettings().first().mazhab)
            for (mazhab in listOf(Mazhab.HANAFI, Mazhab.SHAFI)) {
                repository.updateMazhab(mazhab)
                job.cancelAndJoin()
                job = Job()
                repository = repository()
                assertEquals(mazhab, repository.getSettings().first().mazhab)
            }
            assertEquals(2, invalidations)
        } finally {
            job.cancelAndJoin()
            directory.deleteRecursively()
        }
    }

    @Test
    fun `device default applies only when no face has been selected`() = runBlocking {
        for (defaultFace in listOf("mobile", "classic")) {
            val directory = Files.createTempDirectory("clock-default").toFile()
            val job = Job()
            try {
                val repository = SettingsRepository(
                    PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) {
                        directory.resolve("settings.preferences_pb")
                    },
                    Provider<PrayerTimesCacheInvalidator> { error("Unexpected cache invalidation") },
                    defaultScreenFaceId = defaultFace,
                )
                assertEquals(defaultFace, repository.getSettings().first().screenFaceId)
                repository.updateScreenFaceId("mihrab")
                assertEquals("mihrab", repository.getSettings().first().screenFaceId)
                repository.updateScreenFaceId("classic")
                assertEquals("classic", repository.getSettings().first().screenFaceId)
            } finally {
                job.cancelAndJoin()
                directory.deleteRecursively()
            }
        }
    }

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
