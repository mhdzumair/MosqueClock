package com.mosque.prayerclock

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.service.ApkDownloader
import com.mosque.prayerclock.data.service.UpdateChecker
import com.mosque.prayerclock.data.service.UpdateInfo
import com.mosque.prayerclock.service.PrayerNotificationService
import com.mosque.prayerclock.ui.LocalizedApp
import com.mosque.prayerclock.ui.components.UpdateDialog
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.screens.MainScreen
import com.mosque.prayerclock.ui.screens.SettingsScreen
import com.mosque.prayerclock.ui.theme.AppColorThemes
import com.mosque.prayerclock.ui.theme.MosqueClockTheme
import com.mosque.prayerclock.viewmodel.MainViewModel
import com.mosque.prayerclock.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if app was started from boot
        val startedFromBoot = intent.getBooleanExtra("started_from_boot", false)
        if (startedFromBoot) {
            // Log that app was started from boot for debugging
            android.util.Log.d("MainActivity", "App started from device boot")
        }

        // Configure window for always-on display
        configureDisplaySettings()

        // Request overlay permission for auto-start functionality
        requestOverlayPermission()

        // Start prayer notification service for sound alerts
        startPrayerNotificationService()

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            // Get the selected color theme
            val selectedColorTheme = AppColorThemes.getThemeById(settings.colorTheme)

            MosqueClockTheme(colorTheme = selectedColorTheme) {
                var effectiveLanguage by remember { mutableStateOf(settings.language) }
                LaunchedEffect(settings.language) {
                    if (settings.language == com.mosque.prayerclock.data.model.Language.MULTI) {
                        val cycle =
                            listOf(
                                com.mosque.prayerclock.data.model.Language.ENGLISH,
                                com.mosque.prayerclock.data.model.Language.TAMIL,
                                com.mosque.prayerclock.data.model.Language.SINHALA,
                            )
                        var index = 0
                        while (true) {
                            effectiveLanguage = cycle[index % cycle.size]
                            index++
                            // Much longer delay to minimize interference with countdown timer
                            kotlinx.coroutines.delay(30_000) // 30 seconds - minimal frequency for smooth countdown
                        }
                    } else {
                        effectiveLanguage = settings.language
                    }
                }
                LocalizedApp(language = effectiveLanguage) {
                    MosqueClockApp(
                        onExitApp = {
                            finish()
                            exitProcess(0)
                        },
                    )
                }
            }
        }
    }

    private fun requestOverlayPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    android.util.Log.d("MainActivity", "Requesting overlay permission...")
                    val intent =
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"),
                        )

                    // Check if the intent can be handled before starting it
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                        android.util.Log.d("MainActivity", "Overlay permission dialog opened")
                    } else {
                        android.util.Log.w(
                            "MainActivity",
                            "Overlay permission settings not available on this device (Android TV)",
                        )
                    }
                } else {
                    android.util.Log.d("MainActivity", "Overlay permission already granted")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error requesting overlay permission", e)
        }
    }

    private fun configureDisplaySettings() {
        try {
            // Keep screen on and prevent sleep
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Use modern approach for showing over lock screen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                // Fallback for older versions
                @Suppress("DEPRECATION")
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                @Suppress("DEPRECATION")
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                @Suppress("DEPRECATION")
                window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            }

            android.util.Log.d("MainActivity", "Display settings configured for always-on mode")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error configuring display settings", e)
        }
    }

    private fun startPrayerNotificationService() {
        try {
            val serviceIntent = Intent(this, PrayerNotificationService::class.java)
            startService(serviceIntent)
            android.util.Log.d("MainActivity", "Prayer notification service started")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error starting prayer notification service", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Optional: Put TV to sleep when app is destroyed
        // Uncomment the line below if you want TV to sleep when app closes
        // App destroyed - cleanup handled by system
    }

    override fun onPause() {
        super.onPause()
        // App paused - display management handled by window flags
    }

    override fun onResume() {
        super.onResume()
        // Display management handled by window flags
    }
}

@Composable
fun MosqueClockApp(onExitApp: () -> Unit = {}) {
    val navController = rememberNavController()
    var showExitDialog by remember { mutableStateOf(false) }
    
    // Auto-update state
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    // Self-update feature only available for GitHub/sideload distribution
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val apkDownloader = remember { ApkDownloader() }
    val updateChecker = remember { UpdateChecker() }
    
    // Check for updates on startup (only once) - GitHub distribution only
    var hasCheckedForUpdates by remember { mutableStateOf(false) }
    
    LaunchedEffect(settings.autoUpdateCheckEnabled, hasCheckedForUpdates) {
        // Only check for updates if self-update is enabled (GitHub builds)
        if (BuildConfig.ENABLE_SELF_UPDATE && settings.autoUpdateCheckEnabled && !hasCheckedForUpdates) {
            hasCheckedForUpdates = true
            // Small delay to let the app initialize first
            delay(2000)
            
            try {
                val currentVersion = BuildConfig.VERSION_NAME
                val result = updateChecker.checkForUpdates(currentVersion)
                
                if (result != null && result.hasUpdate) {
                    // Check if this version was skipped by user
                    if (settings.skippedUpdateVersion != result.latestVersion) {
                        updateInfo = result
                        showUpdateDialog = true
                        android.util.Log.d("MosqueClockApp", "Update available: ${result.latestVersion}")
                    } else {
                        android.util.Log.d("MosqueClockApp", "Update ${result.latestVersion} was skipped by user")
                    }
                } else {
                    android.util.Log.d("MosqueClockApp", "No updates available or check failed")
                }
            } catch (e: Exception) {
                android.util.Log.e("MosqueClockApp", "Error checking for updates", e)
            }
        }
    }

    // Handle back button properly for TV
    BackHandler(enabled = true) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        when (currentRoute) {
            "settings" -> navController.popBackStack()
            "main" -> showExitDialog = true
            else -> showExitDialog = true
        }
    }

    Box(
        modifier =
            Modifier.fillMaxSize().onKeyEvent { keyEvent ->
                when (keyEvent.key) {
                    Key.Menu, Key.DirectionCenter -> {
                        // Menu key or center D-pad to open settings
                        if (navController.currentBackStackEntry?.destination?.route ==
                            "main"
                        ) {
                            navController.navigate("settings")
                            true
                        } else {
                            false
                        }
                    }
                    Key.Back -> {
                        val currentRoute =
                            navController.currentBackStackEntry?.destination?.route
                        when (currentRoute) {
                            "settings" -> {
                                navController.popBackStack()
                                true
                            }
                            "main" -> {
                                showExitDialog = true
                                true
                            }
                            else -> false
                        }
                    }
                    else -> false
                }
            },
    ) {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(onOpenSettings = { navController.navigate("settings") })
            }

            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }

        // Exit confirmation dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = {
                    Text(
                        text = localizedStringResource(R.string.exit_app),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                text = { Text(localizedStringResource(R.string.exit_confirmation)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            onExitApp()
                        },
                    ) { Text(localizedStringResource(R.string.exit)) }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text(localizedStringResource(R.string.cancel))
                    }
                },
            )
        }
        
        // Auto-update dialog (GitHub distribution only)
        if (BuildConfig.ENABLE_SELF_UPDATE && showUpdateDialog && updateInfo != null) {
            UpdateDialog(
                updateInfo = updateInfo!!,
                apkDownloader = apkDownloader,
                onDismiss = { showUpdateDialog = false },
                onSkipVersion = { version ->
                    scope.launch {
                        settingsViewModel.updateSkippedUpdateVersion(version)
                    }
                },
            )
        }
    }
}
