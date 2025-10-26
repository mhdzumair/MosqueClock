package com.mosque.prayerclock.utils

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Helper class for checking and requesting app permissions
 */
object PermissionsHelper {
    /**
     * Permission type enum
     */
    enum class PermissionType {
        RUNTIME, // Can request via ActivityResultContracts.RequestPermission
        OVERLAY, // Requires Settings.ACTION_MANAGE_OVERLAY_PERMISSION
        INSTALL_PACKAGES, // Requires Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
        DEFAULT_LAUNCHER, // Requires Settings.ACTION_HOME_SETTINGS
        BATTERY_OPTIMIZATION, // Requires Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        SYSTEM_SETTINGS, // Requires opening system settings
    }

    /**
     * Data class representing a permission status
     */
    data class PermissionStatus(
        val name: String,
        val description: String,
        val isGranted: Boolean,
        val isRequired: Boolean,
        val permissionType: PermissionType,
        val permissionName: String? = null, // For runtime permissions
        val settingsIntent: Intent? = null,
    )

    /**
     * Check if app is set as default launcher
     */
    fun isDefaultLauncher(context: Context): Boolean =
        LauncherHelper.isDefaultLauncher(context)

    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Notifications are enabled by default on older versions
            true
        }

    /**
     * Check if app can display over other apps (overlay permission)
     */
    fun hasOverlayPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

    /**
     * Check if app has install packages permission
     */
    fun hasInstallPackagesPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    /**
     * Check if app has storage permission (for older Android versions)
     */
    fun hasStoragePermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Not needed on Android 10+
            true
        }

    /**
     * Check if app has audio access permission (Android 13+)
     */
    fun hasAudioPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12: Need READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Below Android 6: Permission granted by default
            true
        }

    /**
     * Check if app has boot completed permission
     */
    fun hasBootPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Check if notifications are enabled (system level)
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }

    /**
     * Get all permission statuses
     */
    fun getAllPermissionStatuses(context: Context): List<PermissionStatus> {
        val permissions = mutableListOf<PermissionStatus>()

        // Default Launcher
        permissions.add(
            PermissionStatus(
                name = "Default Launcher",
                description = "Set as home screen launcher",
                isGranted = isDefaultLauncher(context),
                isRequired = true,
                permissionType = PermissionType.DEFAULT_LAUNCHER,
                settingsIntent =
                    Intent(Settings.ACTION_HOME_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    },
            ),
        )

        // Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(
                PermissionStatus(
                    name = "Notification Permission",
                    description = "Show prayer time notifications",
                    isGranted = hasNotificationPermission(context),
                    isRequired = false,
                    permissionType = PermissionType.RUNTIME,
                    permissionName = Manifest.permission.POST_NOTIFICATIONS,
                    settingsIntent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                ),
            )
        }

        // Notifications Enabled
        permissions.add(
            PermissionStatus(
                name = "Notifications Enabled",
                description = "Allow app to show notifications",
                isGranted = areNotificationsEnabled(context),
                isRequired = false,
                permissionType = PermissionType.SYSTEM_SETTINGS,
                settingsIntent =
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    },
            ),
        )

        // Overlay Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.add(
                PermissionStatus(
                    name = "Display Over Other Apps",
                    description = "Keep app always on top",
                    isGranted = hasOverlayPermission(context),
                    isRequired = false,
                    permissionType = PermissionType.OVERLAY,
                    settingsIntent =
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                ),
            )
        }

        // Install Packages Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissions.add(
                PermissionStatus(
                    name = "Install Unknown Apps",
                    description = "Install app updates",
                    isGranted = hasInstallPackagesPermission(context),
                    isRequired = false,
                    permissionType = PermissionType.INSTALL_PACKAGES,
                    settingsIntent =
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                ),
            )
        }

        // Storage Permission (Android 9 and below)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(
                PermissionStatus(
                    name = "Storage Access",
                    description = "Download APK updates and access audio files",
                    isGranted = hasStoragePermission(context),
                    isRequired = false,
                    permissionType = PermissionType.RUNTIME,
                    permissionName = Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    settingsIntent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                ),
            )
        }

        // Audio Access Permission (Android 13+) or READ_EXTERNAL_STORAGE (Android 10-12)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(
                PermissionStatus(
                    name = "Audio File Access",
                    description = "Access audio files for custom prayer sounds",
                    isGranted = hasAudioPermission(context),
                    isRequired = false,
                    permissionType = PermissionType.RUNTIME,
                    permissionName = Manifest.permission.READ_MEDIA_AUDIO,
                    settingsIntent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                ),
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(
                PermissionStatus(
                    name = "Audio File Access",
                    description = "Access audio files for custom prayer sounds",
                    isGranted = hasAudioPermission(context),
                    isRequired = false,
                    permissionType = PermissionType.RUNTIME,
                    permissionName = Manifest.permission.READ_EXTERNAL_STORAGE,
                    settingsIntent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                ),
            )
        }

        // Boot Permission
        permissions.add(
            PermissionStatus(
                name = "Auto-Start on Boot",
                description = "Launch app automatically when device starts",
                isGranted = hasBootPermission(context),
                isRequired = false,
                permissionType = PermissionType.RUNTIME,
                permissionName = Manifest.permission.RECEIVE_BOOT_COMPLETED,
                settingsIntent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    },
            ),
        )

        // Battery Optimization (Important for always-on apps)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val isIgnoringBatteryOptimizations =
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true

            permissions.add(
                PermissionStatus(
                    name = "Battery Optimization",
                    description = "Allow app to run in background without restrictions",
                    isGranted = isIgnoringBatteryOptimizations,
                    isRequired = false,
                    permissionType = PermissionType.BATTERY_OPTIMIZATION,
                    settingsIntent =
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                ),
            )
        }

        return permissions
    }

    /**
     * Open settings for a specific permission
     */
    fun openPermissionSettings(
        context: Context,
        intent: Intent?,
    ): Boolean =
        try {
            if (intent != null && intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                // Fallback to app settings
                openAppSettings(context)
            }
        } catch (e: Exception) {
            android.util.Log.e("PermissionsHelper", "Failed to open permission settings", e)
            false
        }

    /**
     * Open app settings page
     */
    fun openAppSettings(context: Context): Boolean =
        try {
            val intent =
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            android.util.Log.e("PermissionsHelper", "Failed to open app settings", e)
            false
        }

    /**
     * Get count of granted permissions
     */
    fun getGrantedPermissionsCount(context: Context): Int =
        getAllPermissionStatuses(context).count { it.isGranted }

    /**
     * Get count of total permissions
     */
    fun getTotalPermissionsCount(context: Context): Int = getAllPermissionStatuses(context).size

    /**
     * Check if all required permissions are granted
     */
    fun areAllRequiredPermissionsGranted(context: Context): Boolean =
        getAllPermissionStatuses(context)
            .filter { it.isRequired }
            .all { it.isGranted }

    /**
     * Check if battery optimization is ignored for the app
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        }
        return true // Battery optimization not applicable on older versions
    }

    /**
     * Request battery optimization exemption
     * Returns an intent to open battery optimization settings
     */
    fun getBatteryOptimizationIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        return null
    }

    /**
     * Open battery optimization settings
     */
    fun requestBatteryOptimizationExemption(context: Context): Boolean {
        val intent = getBatteryOptimizationIntent(context)
        return if (intent != null) {
            try {
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                android.util.Log.e("PermissionsHelper", "Failed to open battery optimization settings", e)
                false
            }
        } else {
            false
        }
    }
}

