package com.mosque.prayerclock.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/**
 * Helper class for managing launcher-related operations and providing
 * access to other apps when the app is set as the default launcher.
 */
object LauncherHelper {
    /**
     * Opens the Android Settings app
     */
    fun openAndroidSettings(context: Context): Boolean =
        try {
            val intent =
                Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            android.util.Log.e("LauncherHelper", "Failed to open Android settings", e)
            false
        }

    /**
     * Opens the Date & Time settings
     */
    fun openDateTimeSettings(context: Context): Boolean =
        try {
            val intent =
                Intent(Settings.ACTION_DATE_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback to general settings
            openAndroidSettings(context)
        }

    /**
     * Opens an app chooser to access other applications
     * This shows a list of all installed apps
     */
    fun openAppChooser(context: Context): Boolean =
        try {
            // Get all launchable apps
            val mainIntent =
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

            val packageManager = context.packageManager
            val apps = packageManager.queryIntentActivities(mainIntent, 0)

            if (apps.isNotEmpty()) {
                // Create a chooser with all apps
                val chooser = Intent.createChooser(mainIntent, "Select App")
                chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(chooser)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("LauncherHelper", "Failed to open app chooser", e)
            false
        }

    /**
     * Opens the launcher chooser to temporarily switch to another launcher
     */
    fun openLauncherChooser(context: Context): Boolean =
        try {
            val intent =
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            android.util.Log.e("LauncherHelper", "Failed to open launcher chooser", e)
            false
        }

    /**
     * Opens the default launcher's app drawer (if available)
     */
    fun openAppDrawer(context: Context): Boolean =
        try {
            // Try to find and launch the default launcher's app list
            val intent =
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

            val packageManager = context.packageManager
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

            if (resolveInfo != null && resolveInfo.activityInfo.packageName != context.packageName) {
                // Open the default launcher
                context.startActivity(intent)
                true
            } else {
                // If we are the default launcher, show app chooser instead
                openAppChooser(context)
            }
        } catch (e: Exception) {
            android.util.Log.e("LauncherHelper", "Failed to open app drawer", e)
            false
        }

    /**
     * Opens a file manager app
     */
    fun openFileManager(context: Context): Boolean {
        // Try multiple methods to open file manager

        // Method 1: Try to open a specific file manager if installed
        val fileManagerPackages =
            listOf(
                "com.android.documentsui", // Android's built-in file manager
                "com.google.android.documentsui", // Google Files
                "com.mi.android.globalFileexplorer", // Xiaomi File Manager
                "com.android.fileexplorer", // Generic file explorer
                "tv.twoapps.filebrowser", // TV File Browser
            )

        for (packageName in fileManagerPackages) {
            if (isPackageInstalled(context, packageName)) {
                return try {
                    val intent =
                        context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    if (intent != null) {
                        context.startActivity(intent)
                        return true
                    }
                    false
                } catch (e: Exception) {
                    continue
                }
            }
        }

        // Method 2: Try generic file browser intent
        return try {
            val intent =
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Method 3: Try opening Downloads folder
            try {
                val intent =
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(
                            Uri.parse("content://com.android.externalstorage.documents/root/primary"),
                            "vnd.android.document/directory",
                        )
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                android.util.Log.e("LauncherHelper", "Failed to open file manager", e2)
                false
            }
        }
    }

    /**
     * Opens the home screen settings where user can change default launcher
     */
    fun openLauncherSettings(context: Context): Boolean =
        try {
            val intent =
                Intent(Settings.ACTION_HOME_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback to general settings
            android.util.Log.e("LauncherHelper", "Failed to open launcher settings", e)
            openAndroidSettings(context)
        }

    /**
     * Triggers the launcher selection dialog to set this app as default launcher.
     * This clears the current default launcher preference and shows the chooser.
     */
    fun setAsDefaultLauncher(context: Context): Boolean {
        return try {
            // Method 1: Try to open home settings where user can select default launcher
            try {
                val intent =
                    Intent(Settings.ACTION_HOME_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                android.util.Log.d("LauncherHelper", "ACTION_HOME_SETTINGS not available, trying alternative", e)
            }

            // Method 2: Trigger launcher chooser by simulating home button press
            // This will show the launcher selection dialog
            val intent =
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            android.util.Log.e("LauncherHelper", "Failed to trigger launcher selection", e)
            false
        }
    }

    /**
     * Checks if this app is currently set as the default launcher
     */
    fun isDefaultLauncher(context: Context): Boolean =
        try {
            val intent =
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                }

            val packageManager = context.packageManager
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

            resolveInfo?.activityInfo?.packageName == context.packageName
        } catch (e: Exception) {
            android.util.Log.e("LauncherHelper", "Failed to check default launcher", e)
            false
        }

    /**
     * Opens a specific app by package name
     */
    fun openApp(
        context: Context,
        packageName: String,
    ): Boolean =
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("LauncherHelper", "Failed to open app: $packageName", e)
            false
        }

    /**
     * Checks if a package is installed
     */
    private fun isPackageInstalled(
        context: Context,
        packageName: String,
    ): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    /**
     * Gets a list of all installed launchers
     */
    fun getInstalledLaunchers(context: Context): List<LauncherInfo> {
        val intent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

        val packageManager = context.packageManager
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)

        return resolveInfos.map { resolveInfo ->
            LauncherInfo(
                packageName = resolveInfo.activityInfo.packageName,
                name = resolveInfo.loadLabel(packageManager).toString(),
                isCurrentApp = resolveInfo.activityInfo.packageName == context.packageName,
            )
        }
    }

    /**
     * Data class representing launcher information
     */
    data class LauncherInfo(
        val packageName: String,
        val name: String,
        val isCurrentApp: Boolean,
    )

    /**
     * Shows a toast with an error message
     */
    private fun showError(
        context: Context,
        message: String,
    ) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
