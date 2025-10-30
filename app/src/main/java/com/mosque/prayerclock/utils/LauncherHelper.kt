package com.mosque.prayerclock.utils

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.Settings
import android.util.Log
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
            Log.e("LauncherHelper", "Failed to open Android settings", e)
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
    fun openAppChooser(context: Context): Boolean {
        return try {
            val packageManager = context.packageManager
            
            // Query for apps with LAUNCHER category (standard Android apps)
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launcherApps = packageManager.queryIntentActivities(launcherIntent, 0)
            
            // Query for apps with LEANBACK_LAUNCHER category (Android TV apps)
            val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            }
            val leanbackApps = packageManager.queryIntentActivities(leanbackIntent, 0)
            
            // Combine both lists and remove duplicates based on package name
            val allApps = (launcherApps + leanbackApps)
                .distinctBy { it.activityInfo.packageName }
                .filter { it.activityInfo.packageName != context.packageName } // Exclude ourselves
                .sortedBy { it.loadLabel(packageManager).toString().lowercase() } // Sort alphabetically
            
            if (allApps.isEmpty()) {
                Toast.makeText(context, "No apps found", Toast.LENGTH_SHORT).show()
                return false
            }
            
            // Create custom dialog for app selection (works better on Android TV)
            val appNames = allApps.map { it.loadLabel(packageManager).toString() }.toTypedArray()
            
            AlertDialog.Builder(context)
                .setTitle("Select App")
                .setItems(appNames) { dialog, which ->
                    val selectedApp = allApps[which]
                    try {
                        // Try to get the default launch intent first
                        val intent = packageManager.getLaunchIntentForPackage(selectedApp.activityInfo.packageName)
                        if (intent != null) {
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } else {
                            // If that fails, create an explicit intent to the activity
                            val explicitIntent = Intent(Intent.ACTION_MAIN).apply {
                                setClassName(
                                    selectedApp.activityInfo.packageName,
                                    selectedApp.activityInfo.name
                                )
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(explicitIntent)
                        }
                    } catch (e: Exception) {
                        Log.e("LauncherHelper", "Failed to launch ${selectedApp.activityInfo.packageName}", e)
                        Toast.makeText(context, "Failed to launch app: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            
            true
        } catch (e: Exception) {
            Log.e("LauncherHelper", "Failed to open app chooser", e)
            Toast.makeText(context, "Failed to open app chooser: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Launch a specific app by its ResolveInfo
     */
    fun launchApp(
        context: Context,
        resolveInfo: ResolveInfo,
    ): Boolean {
        return try {
            val packageManager = context.packageManager
            // Try to get the default launch intent first
            val intent = packageManager.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                true
            } else {
                // If that fails, create an explicit intent to the activity
                val explicitIntent =
                    Intent(Intent.ACTION_MAIN).apply {
                        setClassName(
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name,
                        )
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                context.startActivity(explicitIntent)
                true
            }
        } catch (e: Exception) {
            Log.e("LauncherHelper", "Failed to launch ${resolveInfo.activityInfo.packageName}", e)
            Toast.makeText(context, "Failed to launch app: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
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
            Log.e("LauncherHelper", "Failed to open launcher chooser", e)
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
            Log.e("LauncherHelper", "Failed to open app drawer", e)
            false
        }

    /**
     * Opens a file manager app
     */
    fun openFileManager(context: Context): Boolean {
        // Try multiple methods to open file manager

        // Method 1: Try to open a specific file manager if installed (most reliable)
        val fileManagerPackages =
            listOf(
                // TV/Android box specific
                "com.softwinner.TvdFileManager", // Softwinner TV File Manager (common on Android TV boxes)
                // MiXplorer variants
                "com.mixplorer", // MiXplorer
                "com.mixplorer.silver", // MiXplorer Silver
                "com.mixplorer.beta", // MiXplorer Beta
                // Popular file managers
                "com.google.android.apps.nbu.files", // Files by Google (newer package)
                "com.google.android.documentsui", // Google Files (older)
                "com.android.documentsui", // Android's built-in file manager
                // TV-specific file managers
                "tv.twoapps.filebrowser", // TV File Browser
                "com.lonelycatgames.Xplore", // X-plore File Manager
                "com.speedsoftware.rootexplorer", // Root Explorer
                // Manufacturer-specific
                "com.mi.android.globalFileexplorer", // Xiaomi File Manager
                "com.sec.android.app.myfiles", // Samsung My Files
                "com.asus.filemanager", // ASUS File Manager
                "com.lenovo.FileBrowser2", // Lenovo File Manager
                // Popular third-party
                "com.estrongs.android.pop", // ES File Explorer
                "com.alphainventor.filemanager", // File Manager
                "com.rhmsoft.fm", // Solid Explorer
                "nextapp.fx", // FX File Explorer
                "com.cxinventor.file.explorer", // CX File Explorer
                "com.ghisler.android.TotalCommander", // Total Commander
                "com.mobisystems.fileman", // File Commander
                // Generic/other
                "com.android.fileexplorer", // Generic file explorer
                "com.metago.astro", // ASTRO File Manager
            )

        for (packageName in fileManagerPackages) {
            if (isPackageInstalled(context, packageName)) {
                try {
                    val intent =
                        context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    if (intent != null) {
                        context.startActivity(intent)
                        return true
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        }

        // Method 2: Try opening recent apps/all apps screen (for Android TV)
        try {
            val intent =
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_FILES)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return true
            }
        } catch (e: Exception) {
            // Continue to show message
        }

        // No file manager found - show helpful message
        Toast.makeText(
            context,
            "No file manager found. Downloaded APKs are in: /storage/emulated/0/Download/",
            Toast.LENGTH_LONG
        ).show()
        
        return false
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
            Log.e("LauncherHelper", "Failed to open launcher settings", e)
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
                Log.d("LauncherHelper", "ACTION_HOME_SETTINGS not available, trying alternative", e)
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
            Log.e("LauncherHelper", "Failed to trigger launcher selection", e)
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
            Log.e("LauncherHelper", "Failed to check default launcher", e)
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
            Log.e("LauncherHelper", "Failed to open app: $packageName", e)
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
