package com.mosque.prayerclock.data.service

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.mosque.prayerclock.utils.PermissionsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val downloadId: Long = -1,
    val progress: Int = 0,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val version: String? = null,
    val errorMessage: String? = null,
)

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

@Singleton
class ApkDownloader
    @Inject
    constructor() {
        companion object {
            // Both the startup dialog and Settings observe the same transfer.
            private val progressState = MutableStateFlow(DownloadProgress())
            private val transferLock = Any()
            @Volatile private var transferId = 0L
            private var transferJob: Job? = null
            private var activeCall: Call? = null
            private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            private val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.MINUTES)
                .build()
        }

        val downloadProgress: StateFlow<DownloadProgress> = progressState

        /**
         * Check if APK file is already downloaded
         */
        fun isApkAlreadyDownloaded(
            context: Context,
            version: String,
        ): Boolean {
            val fileName = "MosqueClock-v$version.apk"
            val file = File(context.getExternalFilesDir(null), fileName)
            return isValidApk(context, file, version)
        }

        /**
         * Get the file name for a version
         */
        fun getApkFileName(version: String): String = "MosqueClock-v$version.apk"

        /**
         * Check if app has permission to install packages
         * Required for Android 8.0 (API 26) and above
         */
        fun canInstallPackages(context: Context): Boolean =
            PermissionsHelper.hasInstallPackagesPermission(context)

        /**
         * Request install packages permission
         * Opens the system settings page where user can grant the permission
         */
        fun requestInstallPermission(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val intent =
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }

                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                        Log.d("ApkDownloader", "Opening install permission settings")
                    } else {
                        Log.w("ApkDownloader", "Install permission settings not available on this device")
                        Toast
                            .makeText(
                                context,
                                "Cannot open install permission settings. Please enable manually in Settings > Apps > Special access > Install unknown apps",
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                } catch (e: Exception) {
                    Log.e("ApkDownloader", "Error opening install permission settings", e)
                    Toast
                        .makeText(
                            context,
                            "Error opening settings: ${e.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }

        /**
         * Clean up old APK files to save space
         * Keeps only the current version and latest downloaded version
         */
        fun cleanupOldApkFiles(context: Context, currentVersion: String, keepVersion: String? = null) {
            try {
                val apkDir = context.getExternalFilesDir(null) ?: return
                val apkFiles = apkDir.listFiles { file ->
                    file.name.startsWith("MosqueClock-v") && file.name.endsWith(".apk")
                } ?: return

                val currentFileName = "MosqueClock-v$currentVersion.apk"
                
                apkFiles.forEach { file ->
                    // Don't delete the current version APK
                    if (file.name != currentFileName && file.name != keepVersion?.let(::getApkFileName)) {
                        val deleted = file.delete()
                        if (deleted) {
                            Log.d("ApkDownloader", "Cleaned up old APK: ${file.name}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ApkDownloader", "Error cleaning up old APK files", e)
            }
        }

        /** Stream the user-requested APK directly, without a DownloadManager job. */
        fun downloadApk(
            context: Context,
            downloadUrl: String,
            version: String,
            onComplete: (() -> Unit)? = null,
            installAfterDownload: Boolean = true,
        ) {
            val appContext = context.applicationContext
            synchronized(transferLock) {
                activeCall?.cancel()
                transferJob?.cancel()
                val id = ++transferId
                progressState.value = DownloadProgress(downloadId = id, version = version, status = DownloadStatus.DOWNLOADING)
                transferJob = scope.launch {
                    var partial: File? = null
                    try {
                        require(version.matches(Regex("[A-Za-z0-9._-]+"))) { "Invalid update version" }
                        val directory = appContext.getExternalFilesDir(null)
                            ?: throw IOException("Update storage is unavailable")
                        val fileName = getApkFileName(version)
                        val destination = File(directory, fileName)
                        val temporary = File(directory, "$fileName.$id.partial.apk")
                        partial = temporary
                        removeLegacyDownloads(appContext)
                        ensureActive()
                        val call = client.newCall(Request.Builder().url(downloadUrl).build())
                        synchronized(transferLock) {
                            ensureActive()
                            if (transferId != id) throw CancellationException()
                            activeCall = call
                        }
                        var bytesDownloaded = 0L
                        var totalBytes = 0L
                        call.execute().use { response ->
                            if (!response.isSuccessful) throw IOException("Download server returned HTTP ${response.code}")
                            val body = response.body ?: throw IOException("Download server returned no APK")
                            totalBytes = body.contentLength().coerceAtLeast(0)
                            var lastProgressUpdate = 0L
                            body.byteStream().use { input ->
                                temporary.outputStream().buffered().use { output ->
                                    val buffer = ByteArray(32 * 1024)
                                    while (true) {
                                        ensureActive()
                                        val count = input.read(buffer)
                                        if (count == -1) break
                                        output.write(buffer, 0, count)
                                        bytesDownloaded += count
                                        val now = android.os.SystemClock.elapsedRealtime()
                                        if (now - lastProgressUpdate >= 200) {
                                            synchronized(transferLock) {
                                                if (transferId == id) progressState.value = DownloadProgress(
                                                    downloadId = id, version = version,
                                                    status = DownloadStatus.DOWNLOADING,
                                                    bytesDownloaded = bytesDownloaded, totalBytes = totalBytes,
                                                    progress = if (totalBytes > 0) ((bytesDownloaded * 100 / totalBytes).toInt()).coerceIn(0, 99) else 0,
                                                )
                                            }
                                            lastProgressUpdate = now
                                        }
                                    }
                                }
                            }
                        }
                        ensureActive()
                        if (bytesDownloaded == 0L || (totalBytes > 0 && bytesDownloaded != totalBytes)) {
                            throw IOException("The APK download is incomplete. Please retry.")
                        }
                        if (!isValidApk(appContext, temporary, version)) {
                            throw IOException("The downloaded file is not a valid update for this app.")
                        }
                        synchronized(transferLock) {
                            ensureActive()
                            if (transferId != id) throw CancellationException()
                            if (destination.exists() && !destination.delete()) throw IOException("Cannot replace the previous APK")
                            if (!temporary.renameTo(destination)) throw IOException("Cannot save the downloaded APK")
                            progressState.value = DownloadProgress(
                                downloadId = id, version = version, status = DownloadStatus.COMPLETED,
                                progress = 100, bytesDownloaded = bytesDownloaded, totalBytes = bytesDownloaded,
                            )
                        }
                        withContext(Dispatchers.Main) {
                            if (transferId == id) {
                                onComplete?.invoke()
                                if (installAfterDownload) installApk(appContext, fileName)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        ensureActive()
                        synchronized(transferLock) {
                            if (transferId == id) progressState.value = progressState.value.copy(
                                status = DownloadStatus.FAILED, errorMessage = e.message ?: "Download failed. Please retry.",
                            )
                        }
                        Log.e("ApkDownloader", "APK download failed", e)
                    } finally {
                        partial?.delete()
                        synchronized(transferLock) {
                            if (transferId == id) {
                                activeCall = null
                                transferJob = null
                            }
                        }
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        private fun isValidApk(context: Context, file: File, version: String): Boolean {
            if (!file.isFile || file.length() == 0L) return false
            val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0) ?: return false
            val expectedPackage = context.packageName.removeSuffix(".debug")
            return archive.packageName == expectedPackage && archive.versionName == version
        }

        /** Remove only this app's queued updater jobs from the previous implementation. */
        private fun removeLegacyDownloads(context: Context) {
            try {
                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val ids = mutableListOf<Long>()
                val query = DownloadManager.Query().setFilterByStatus(
                    DownloadManager.STATUS_PENDING or DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_RUNNING,
                )
                manager.query(query)?.use { cursor ->
                    val uriColumn = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_URI)
                    val idColumn = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
                    while (cursor.moveToNext()) {
                        if (cursor.getString(uriColumn).startsWith("https://github.com/mhdzumair/MosqueClock/releases/download/")) {
                            ids.add(cursor.getLong(idColumn))
                        }
                    }
                }
                if (ids.isNotEmpty()) manager.remove(*ids.toLongArray())
            } catch (e: Exception) {
                Log.w("ApkDownloader", "Could not remove old download jobs", e)
            }
        }

        fun cancelDownload(context: Context) {
            synchronized(transferLock) {
                if (progressState.value.status != DownloadStatus.DOWNLOADING) return
                ++transferId
                activeCall?.cancel()
                transferJob?.cancel()
                activeCall = null
                transferJob = null
                progressState.value = progressState.value.copy(status = DownloadStatus.CANCELLED)
            }
        }

        fun installDownloadedApk(context: Context, version: String) {
            installApk(context.applicationContext, getApkFileName(version))
        }

        /**
         * Install APK after download completes
         */
        private fun installApk(
            context: Context,
            fileName: String,
        ) {
            // Launch installation in a coroutine with a small delay to ensure file is fully written
            scope.launch(Dispatchers.Main) {
                try {
                    // Small delay to ensure file is completely written to disk
                    delay(500)

                    // Check if app has install permission (Android 8.0+)
                    if (!canInstallPackages(context)) {
                        Log.w("ApkDownloader", "Install packages permission not granted - redirecting to settings")
                        launch(Dispatchers.Main) {
                            Toast
                                .makeText(
                                    context,
                                    "Please allow installing from this source to update the app",
                                    Toast.LENGTH_LONG,
                                ).show()
                        }
                        // Open the permission settings page instead of attempting to install
                        requestInstallPermission(context)
                        return@launch
                    }

                    val file = File(context.getExternalFilesDir(null), fileName)

                    if (!file.exists()) {
                        Log.e("ApkDownloader", "APK file not found: ${file.absolutePath}")
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Download failed: File not found", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // Verify file is readable and has content
                    if (!file.canRead()) {
                        Log.e("ApkDownloader", "APK file is not readable: ${file.absolutePath}")
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Cannot read APK file", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    if (file.length() == 0L) {
                        Log.e("ApkDownloader", "APK file is empty: ${file.absolutePath}")
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Downloaded file is empty", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    Log.d("ApkDownloader", "Installing APK from: ${file.absolutePath} (${file.length()} bytes)")

                    val uri: Uri =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            // Use FileProvider for Android N and above
                            try {
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file,
                                )
                            } catch (e: IllegalArgumentException) {
                                Log.e(
                                    "ApkDownloader",
                                    "FileProvider failed - file path not covered by provider paths",
                                    e,
                                )
                                Log.e("ApkDownloader", "File absolute path: ${file.absolutePath}")
                                Log.e("ApkDownloader", "File canonical path: ${file.canonicalPath}")
                                launch(Dispatchers.Main) {
                                    Toast
                                        .makeText(
                                            context,
                                            "FileProvider error: ${e.message}",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                }
                                return@launch
                            }
                        } else {
                            Uri.fromFile(file)
                        }

                    Log.d("ApkDownloader", "Generated URI: $uri")

                    // Use ACTION_INSTALL_PACKAGE for better compatibility (Android 8.0+)
                    val installIntent =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                                setDataAndType(uri, "application/vnd.android.package-archive")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                            }
                        } else {
                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/vnd.android.package-archive")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        }

                    // Verify that there's an activity to handle the intent
                    if (installIntent.resolveActivity(context.packageManager) == null) {
                        Log.e("ApkDownloader", "No activity found to handle install intent")
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "No installer app found on this device", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    context.startActivity(installIntent)
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Opening installer...", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ApkDownloader", "Error installing APK", e)
                    Log.e("ApkDownloader", "Exception type: ${e.javaClass.name}")
                    Log.e("ApkDownloader", "Exception message: ${e.message}")
                    e.printStackTrace()
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Installation failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        /**
         * Format bytes to human-readable string
         */
        fun formatBytes(bytes: Long): String =
            when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                else -> "${bytes / (1024 * 1024)} MB"
            }
    }
