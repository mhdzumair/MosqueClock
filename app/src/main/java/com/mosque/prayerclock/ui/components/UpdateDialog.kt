package com.mosque.prayerclock.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mosque.prayerclock.data.service.ApkDownloader
import com.mosque.prayerclock.data.service.DownloadStatus
import com.mosque.prayerclock.data.service.UpdateInfo

/**
 * A dialog component that shows update information and allows one-click installation.
 * Used both on app startup for auto-update checks and in settings for manual update checks.
 */
@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    apkDownloader: ApkDownloader,
    onDismiss: () -> Unit,
    onSkipVersion: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val downloadProgress by apkDownloader.downloadProgress.collectAsState()
    
    var isFileAlreadyDownloaded by remember { mutableStateOf(false) }
    var hasInstallPermission by remember { mutableStateOf(false) }
    
    // Check install permission and if APK is already downloaded
    LaunchedEffect(updateInfo) {
        hasInstallPermission = apkDownloader.canInstallPackages(context)
        isFileAlreadyDownloaded = apkDownloader.isApkAlreadyDownloaded(
            context = context,
            version = updateInfo.latestVersion,
        )
    }
    
    val isDownloading = downloadProgress.status == DownloadStatus.DOWNLOADING
    val isDownloadComplete = downloadProgress.status == DownloadStatus.COMPLETED || isFileAlreadyDownloaded
    
    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) {
                onDismiss()
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = "Update Available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Version info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "New Version",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            )
                            Text(
                                text = "v${updateInfo.latestVersion}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Current",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            )
                            Text(
                                text = "v${updateInfo.currentVersion}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
                
                // Release notes (if available)
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Text(
                        text = "What's New:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Text(
                            text = updateInfo.releaseNotes.take(500) + 
                                if (updateInfo.releaseNotes.length > 500) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                
                // Install permission warning (Android 8.0+)
                if (!hasInstallPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Permission Required",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = "Grant install permission to update the app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
                
                // Download progress
                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Downloading...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${downloadProgress.progress}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { downloadProgress.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (downloadProgress.totalBytes > 0) {
                            Text(
                                text = "${apkDownloader.formatBytes(downloadProgress.bytesDownloaded)} / ${apkDownloader.formatBytes(downloadProgress.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                !hasInstallPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    Button(
                        onClick = {
                            apkDownloader.requestInstallPermission(context)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Permission")
                    }
                }
                isDownloading -> {
                    Button(
                        onClick = { /* Disabled while downloading */ },
                        enabled = false,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Downloading...")
                    }
                }
                isDownloadComplete -> {
                    Button(
                        onClick = {
                            // Clean up old APKs before installing
                            apkDownloader.cleanupOldApkFiles(context, updateInfo.currentVersion)
                            
                            // Trigger installation from already downloaded file
                            val fileName = apkDownloader.getApkFileName(updateInfo.latestVersion)
                            val file = java.io.File(context.getExternalFilesDir(null), fileName)
                            
                            if (file.exists()) {
                                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file,
                                    )
                                } else {
                                    android.net.Uri.fromFile(file)
                                }
                                
                                val installIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    android.content.Intent(android.content.Intent.ACTION_INSTALL_PACKAGE).apply {
                                        setDataAndType(uri, "application/vnd.android.package-archive")
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        putExtra(android.content.Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                                        putExtra(android.content.Intent.EXTRA_RETURN_RESULT, true)
                                    }
                                } else {
                                    android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/vnd.android.package-archive")
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                }
                                
                                context.startActivity(installIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.InstallMobile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Install Now")
                    }
                }
                else -> {
                    Button(
                        onClick = {
                            apkDownloader.downloadApk(
                                context = context,
                                downloadUrl = updateInfo.downloadUrl,
                                version = updateInfo.latestVersion,
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download & Install")
                    }
                }
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Skip this version button (only show if callback is provided)
                if (onSkipVersion != null && !isDownloading) {
                    TextButton(
                        onClick = {
                            onSkipVersion(updateInfo.latestVersion)
                            onDismiss()
                        },
                    ) {
                        Text("Skip Version")
                    }
                }
                
                // Later/Cancel button
                if (!isDownloading) {
                    OutlinedButton(
                        onClick = onDismiss,
                    ) {
                        Text("Later")
                    }
                }
            }
        },
    )
}

