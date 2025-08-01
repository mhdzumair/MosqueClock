package com.mosque.prayerclock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mosque.prayerclock.ui.components.AnalogClock
import com.mosque.prayerclock.ui.components.DigitalClock
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
        
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            
            MosqueClockTheme(theme = settings.theme) {
                MosqueClockApp(
                    onExitApp = {
                        finish()
                        exitProcess(0)
                    }
                )
            }
        }
    }
}

@Composable
fun MosqueClockApp(
    onExitApp: () -> Unit = {}
) {
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
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { keyEvent ->
                when (keyEvent.key) {
                    Key.Menu, Key.DirectionCenter -> {
                        // Menu key or center D-pad to open settings
                        if (navController.currentBackStackEntry?.destination?.route == "main") {
                            navController.navigate("settings")
                            true
                        } else {
                            false
                        }
                    }
                    Key.Back -> {
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
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
            }
    ) {
        NavHost(
            navController = navController,
            startDestination = "main"
        ) {
            composable("main") {
                MainScreen(
                    onOpenSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onExitApp = {
                        showExitDialog = true
                    }
                )
            }
        }
        
        // Exit confirmation dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = {
                    Text(
                        text = "Exit App",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Text("Are you sure you want to exit the Mosque Prayer Clock?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            onExitApp()
                        }
                    ) {
                        Text("Exit")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showExitDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

