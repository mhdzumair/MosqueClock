package com.mosque.prayerclock

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mosque.prayerclock.ui.LocalizedApp
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.screens.MainScreen
import com.mosque.prayerclock.ui.screens.SettingsScreen
import com.mosque.prayerclock.ui.theme.MosqueClockTheme
import com.mosque.prayerclock.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
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

        // Request overlay permission for auto-start functionality
        requestOverlayPermission()

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            MosqueClockTheme {
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
                            kotlinx.coroutines.delay(10_000)
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
}

@Composable
fun MosqueClockApp(onExitApp: () -> Unit = {}) {
    val navController = rememberNavController()
    var showExitDialog by remember { mutableStateOf(false) }

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
                    onExitApp = { showExitDialog = true },
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
    }
}
