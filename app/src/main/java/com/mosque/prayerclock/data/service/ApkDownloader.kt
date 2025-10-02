package com.mosque.prayerclock.data.service

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

        /**
         * Download APK from the given URL
         */
        fun downloadApk(
            context: Context,
            downloadUrl: String,
            version: String,
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
                    _downloadProgress.value =
                        DownloadProgress(
                            downloadId = downloadId,
                            status = DownloadStatus.DOWNLOADING,
                        )

                    // Register download complete receiver
                    registerDownloadCompleteReceiver(context, fileName)

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
         * Get current download progress
         */
        fun getDownloadProgress(context: Context): DownloadProgress? {
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
            try {
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

                if (!file.exists()) {
                    Log.e("ApkDownloader", "APK file not found: ${file.absolutePath}")
                    Toast.makeText(context, "Download failed: File not found", Toast.LENGTH_SHORT).show()
                    return
                }

                Log.d("ApkDownloader", "Installing APK from: ${file.absolutePath}")

                val uri: Uri =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // Use FileProvider for Android N and above
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )
                    } else {
                        Uri.fromFile(file)
                    }

                val installIntent =
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }

                context.startActivity(installIntent)
                Toast.makeText(context, "Opening installer...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("ApkDownloader", "Error installing APK", e)
                Toast.makeText(context, "Installation failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        /**
         * Register broadcast receiver for download completion
         */
        private fun registerDownloadCompleteReceiver(
            context: Context,
            fileName: String,
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
            context.registerReceiver(onDownloadCompleteReceiver, filter)
        }

        /**
         * Format bytes to human-readable string
         */
        fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                else -> "${bytes / (1024 * 1024)} MB"
            }
        }
    }

