package com.mosque.prayerclock.data.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Opt-in network checks: run with instrumentation argument verifyLiveUpdate=true. */
@RunWith(AndroidJUnit4::class)
class ApkDownloaderDeviceTest {
    @Test
    fun releasedApkDownloadsAndValidatesWithoutDownloadManager() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("verifyLiveUpdate") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val downloader = ApkDownloader()
        val version = "2.0.1"
        val target = File(context.getExternalFilesDir(null), downloader.getApkFileName(version))
        try {
            downloader.downloadApk(
                context, "https://github.com/mhdzumair/MosqueClock/releases/download/v2.0.1/MosqueClock-v2.0.1.apk",
                version, installAfterDownload = false,
            )
            val result = withTimeout(120_000) {
                downloader.downloadProgress.first { it.status != DownloadStatus.DOWNLOADING }
            }
            assertEquals(result.errorMessage, DownloadStatus.COMPLETED, result.status)
            assertEquals(100, result.progress)
            assertEquals(11_618_824L, result.bytesDownloaded)
            assertTrue(downloader.isApkAlreadyDownloaded(context, version))
            // The dialog and Settings must observe the same completed transfer.
            assertEquals(result, ApkDownloader().downloadProgress.value)
        } finally {
            downloader.cancelDownload(context)
            target.delete()
        }
    }

    @Test
    fun failedDownloadLeavesRetryableStateAndNoPartialApk() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("verifyLiveUpdate") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val downloader = ApkDownloader()
        val version = "missing-updater-test"
        downloader.downloadApk(
            context, "https://github.com/mhdzumair/MosqueClock/releases/download/$version/missing.apk",
            version, installAfterDownload = false,
        )
        val result = withTimeout(60_000) {
            downloader.downloadProgress.first { it.status != DownloadStatus.DOWNLOADING }
        }
        assertEquals(DownloadStatus.FAILED, result.status)
        assertNotNull(result.errorMessage)
        assertTrue(!downloader.isApkAlreadyDownloaded(context, version))
    }
}
