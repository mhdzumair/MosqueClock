package com.mosque.prayerclock

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mosque.prayerclock.data.model.WeatherProvider
import com.mosque.prayerclock.ui.theme.MosqueClockTheme
import com.mosque.prayerclock.viewmodel.MainUiState
import com.mosque.prayerclock.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Optimize for faster startup and TV wake-up
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Configure window flags to wake up display immediately
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Use modern approach for showing over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            // Fallback for older versions
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        setContent {
            MosqueClockTheme {
                val viewModel: MainViewModel = hiltViewModel()
                SplashScreen(
                    viewModel = viewModel,
                    onSplashFinished = {
                        // Start MainActivity and finish splash
                        val intent = Intent(this@SplashActivity, MainActivity::class.java)
                        // Pass through any extras from the original intent
                        intent.putExtras(this@SplashActivity.intent.extras ?: Bundle())
                        startActivity(intent)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
fun SplashScreen(
    viewModel: MainViewModel,
    onSplashFinished: () -> Unit,
) {
    // Collect UI state to monitor loading progress
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Screen transition animation
    var isVisible by remember { mutableStateOf(false) }
    var showAnimatedContent by remember { mutableStateOf(false) }

    val screenAlpha by
        animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = tween(300, easing = EaseOutCubic),
            label = "screen_fade",
        )

    // Smooth transition from static to animated content
    val animatedContentAlpha by
        animateFloatAsState(
            targetValue = if (showAnimatedContent) 1f else 0f,
            animationSpec = tween(500, easing = EaseInOutCubic),
            label = "animated_content_fade",
        )

    // Logo scale animation
    val infiniteTransition = rememberInfiniteTransition(label = "splash_animation")

    val logoScale by
        infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.0f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "logo_scale",
        )

    val logoAlpha by
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.0f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "logo_alpha",
        )

    // Load initial data in splash screen and navigate when complete
    LaunchedEffect(Unit) {
        // Start immediately without delay for faster perceived startup
        isVisible = true

        // Show animated content immediately for better UX
        delay(100) // Minimal delay for smooth transition
        showAnimatedContent = true

        // Load data in background without blocking UI
        viewModel.loadPrayerTimes()
    }

    // Wait for data loading to complete before navigating
    LaunchedEffect(uiState, showAnimatedContent) {
        if (showAnimatedContent) {
            when (uiState) {
                is MainUiState.Success -> {
                    // Brief delay to show success state
                    delay(500)
                    onSplashFinished()
                }
                is MainUiState.Error -> {
                    // Brief delay to show error state
                    delay(1000)
                    onSplashFinished()
                }
                is MainUiState.Loading -> {
                    // Keep waiting for loading to complete
                }
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .alpha(screenAlpha)
                .background(
                    Brush.radialGradient(
                        colors =
                            listOf(
                                Color(0xFF2E7D32), // green_800
                                Color(0xFF1B5E20), // green_900
                                Color(0xFF000000), // black
                            ),
                        radius = 1000f,
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(animatedContentAlpha),
        ) {
            // Logo with animation
            Image(
                painter = painterResource(id = R.drawable.mosque_logo),
                contentDescription = "Mosque Clock Logo",
                modifier =
                    Modifier
                        .size(width = 350.dp, height = 350.dp)
                        .scale(logoScale)
                        .alpha(logoAlpha),
                contentScale = ContentScale.Fit,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Loading dots
            LoadingDots()

            Spacer(modifier = Modifier.height(16.dp))

            // Loading text based on current state
            val loadingText =
                when (uiState) {
                    is MainUiState.Loading -> "Loading prayer times..."
                    is MainUiState.Success -> "Ready!"
                    is MainUiState.Error -> "Loading failed, continuing..."
                }

            androidx.compose.material3.Text(
                text = loadingText,
                color =
                    when (uiState) {
                        is MainUiState.Success -> Color(0xFF4CAF50) // Green for success
                        is MainUiState.Error -> Color(0xFFFF9800) // Orange for error
                        else -> Color.White.copy(alpha = 0.8f) // White for loading
                    },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(logoAlpha),
            )
        }
    }
}

@Composable
fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_dots")

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha by
                infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(600, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse,
                            initialStartOffset = StartOffset(index * 200),
                        ),
                    label = "dot_alpha_$index",
                )

            val scale by
                infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(600, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse,
                            initialStartOffset = StartOffset(index * 200),
                        ),
                    label = "dot_scale_$index",
                )

            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .scale(scale)
                        .alpha(alpha)
                        .background(
                            color = Color(0xFF4CAF50), // green_500
                            shape = CircleShape,
                        ),
            )
        }
    }
}
