package com.mosque.prayerclock.data.service

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val downloadId: Long = -1,
    val progress: Int = 0,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
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
        private val _downloadProgress = MutableStateFlow(DownloadProgress())
        val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress

        private var downloadId: Long = -1
        private var downloadManager: DownloadManager? = null
        private var onDownloadCompleteReceiver: BroadcastReceiver? = null
        private var progressMonitorJob: Job? = null
        private val scope = CoroutineScope(Dispatchers.IO)

        /**
         * Check if APK file is already downloaded
         */
        fun isApkAlreadyDownloaded(
            context: Context,
            version: String,
        ): Boolean {
            val fileName = "MosqueClock-v$version.apk"
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            return file.exists()
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true // Permission not required for older Android versions
            }

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
         * Download APK from the given URL
         */
        fun downloadApk(
            context: Context,
            downloadUrl: String,
            version: String,
            onComplete: (() -> Unit)? = null,
        ) {
            try {
                Log.d("ApkDownloader", "Starting download from: $downloadUrl")

                // Cancel any existing download
                cancelDownload(context)

                // Initialize download manager
                downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                // Prepare download request
                val fileName = "MosqueClock-v$version.apk"
                val request =
                    DownloadManager
                        .Request(Uri.parse(downloadUrl))
                        .setTitle("MosqueClock Update")
                        .setDescription("Downloading v$version")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)

                // Start download
                downloadId = downloadManager?.enqueue(request) ?: -1

                if (downloadId != -1L) {
                    Log.d("ApkDownloader", "Download started with ID: $downloadId")
                    _downloadProgress.value =
                        DownloadProgress(
                            downloadId = downloadId,
                            status = DownloadStatus.DOWNLOADING,
                        )

                    // Start monitoring download progress (primary completion handler)
                    startProgressMonitoring(context, fileName, onComplete)

                    // Register download complete receiver (backup handler)
                    registerDownloadCompleteReceiver(context, fileName, onComplete)

                    Toast.makeText(context, "Downloading update v$version", Toast.LENGTH_SHORT).show()
                } else {
                    _downloadProgress.value =
                        DownloadProgress(
                            status = DownloadStatus.FAILED,
                        )
                    Toast.makeText(context, "Failed to start download", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ApkDownloader", "Error downloading APK", e)
                _downloadProgress.value =
                    DownloadProgress(
                        status = DownloadStatus.FAILED,
                    )
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Start monitoring download progress in background
         */
        private fun startProgressMonitoring(
            context: Context,
            fileName: String,
            onComplete: (() -> Unit)? = null,
        ) {
            // Cancel existing monitoring job
            progressMonitorJob?.cancel()

            progressMonitorJob =
                scope.launch {
                    while (isActive && downloadId != -1L) {
                        try {
                            val progress = queryDownloadProgress(context)
                            if (progress != null) {
                                _downloadProgress.value = progress

                                Log.d(
                                    "ApkDownloader",
                                    "Progress: ${progress.progress}% " +
                                        "(${formatBytes(
                                            progress.bytesDownloaded,
                                        )}/${formatBytes(progress.totalBytes)}) " +
                                        "Status: ${progress.status}",
                                )

                                // If download completed, handle it directly
                                // (BroadcastReceiver is a backup for this)
                                if (progress.status == DownloadStatus.COMPLETED) {
                                    Log.d(
                                        "ApkDownloader",
                                        "Download completed via progress monitor - triggering installation",
                                    )

                                    // Notify callback
                                    onComplete?.invoke()

                                    // Open installer
                                    installApk(context, fileName)

                                    break
                                }

                                // Stop monitoring if failed
                                if (progress.status == DownloadStatus.FAILED) {
                                    Log.e("ApkDownloader", "Download failed")
                                    break
                                }
                            }
                            delay(500) // Update every 500ms
                        } catch (e: Exception) {
                            Log.e("ApkDownloader", "Error monitoring progress", e)
                            break
                        }
                    }
                }
        }

        /**
         * Get current download progress
         */
        private fun queryDownloadProgress(context: Context): DownloadProgress? {
            if (downloadId == -1L || downloadManager == null) return null

            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor? = downloadManager?.query(query)

            cursor?.use {
                if (it.moveToFirst()) {
                    val bytesDownloadedIndex = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)

                    val bytesDownloaded = if (bytesDownloadedIndex >= 0) it.getLong(bytesDownloadedIndex) else 0L
                    val bytesTotal = if (bytesTotalIndex >= 0) it.getLong(bytesTotalIndex) else 0L
                    val status = if (statusIndex >= 0) it.getInt(statusIndex) else DownloadManager.STATUS_FAILED

                    val progress =
                        if (bytesTotal > 0) {
                            ((bytesDownloaded * 100) / bytesTotal).toInt()
                        } else {
                            0
                        }

                    val downloadStatus =
                        when (status) {
                            DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                            DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                            DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                            DownloadManager.STATUS_PAUSED -> DownloadStatus.DOWNLOADING
                            else -> DownloadStatus.IDLE
                        }

                    return DownloadProgress(
                        downloadId = downloadId,
                        progress = progress,
                        status = downloadStatus,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = bytesTotal,
                    )
                }
            }

            return null
        }

        /**
         * Cancel current download
         */
        fun cancelDownload(context: Context) {
            if (downloadId != -1L) {
                Log.d("ApkDownloader", "Cancelling download: $downloadId")

                // Stop progress monitoring
                progressMonitorJob?.cancel()
                progressMonitorJob = null

                downloadManager?.remove(downloadId)
                downloadId = -1
                _downloadProgress.value = DownloadProgress(status = DownloadStatus.CANCELLED)

                // Unregister receiver
                onDownloadCompleteReceiver?.let {
                    try {
                        context.unregisterReceiver(it)
                    } catch (e: Exception) {
                        // Receiver not registered
                    }
                }
                onDownloadCompleteReceiver = null
            }
        }

        /**
         * Install APK after download completes
         */
        private fun installApk(
            context: Context,
            fileName: String,
        ) {
            // Launch installation in a coroutine with a small delay to ensure file is fully written
            scope.launch {
                try {
                    // Small delay to ensure file is completely written to disk
                    delay(500)

                    // Check if app has install permission (Android 8.0+)
                    if (!canInstallPackages(context)) {
                        Log.w("ApkDownloader", "Install packages permission not granted")
                        launch(Dispatchers.Main) {
                            Toast
                                .makeText(
                                    context,
                                    "Please grant install permission in Settings to install updates",
                                    Toast.LENGTH_LONG,
                                ).show()
                        }
                        // Don't return - continue to attempt installation, which will trigger the system permission dialog
                    }

                    val file =
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

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

                    val installIntent =
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        }

                    // Verify that there's an activity to handle the intent
                    if (installIntent.resolveActivity(context.packageManager) == null) {
                        Log.e("ApkDownloader", "No activity found to handle install intent")
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "No installer app found", Toast.LENGTH_LONG).show()
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
         * Register broadcast receiver for download completion
         */
        private fun registerDownloadCompleteReceiver(
            context: Context,
            fileName: String,
            onComplete: (() -> Unit)?,
        ) {
            // Unregister existing receiver if any
            onDownloadCompleteReceiver?.let {
                try {
                    context.unregisterReceiver(it)
                } catch (e: Exception) {
                    // Receiver not registered
                }
            }

            onDownloadCompleteReceiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        if (id == downloadId) {
                            Log.d("ApkDownloader", "Download completed: $id")
                            _downloadProgress.value =
                                DownloadProgress(
                                    downloadId = id,
                                    progress = 100,
                                    status = DownloadStatus.COMPLETED,
                                )

                            // Notify completion callback
                            onComplete?.invoke()

                            // Automatically open installer
                            installApk(context, fileName)

                            // Unregister receiver
                            try {
                                context.unregisterReceiver(this)
                            } catch (e: Exception) {
                                // Already unregistered
                            }
                        }
                    }
                }

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(onDownloadCompleteReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(onDownloadCompleteReceiver, filter)
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
