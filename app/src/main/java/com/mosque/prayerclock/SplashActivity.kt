package com.mosque.prayerclock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
import com.mosque.prayerclock.ui.theme.MosqueClockTheme
import com.mosque.prayerclock.viewmodel.MainUiState
import com.mosque.prayerclock.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Optimize for faster startup
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

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

    // Start loading prayer times immediately and manage splash timing
    LaunchedEffect(Unit) {
        // Start immediately without delay for faster perceived startup
        isVisible = true
        // Start loading prayer times immediately
        viewModel.loadPrayerTimes()

        // Delay before showing animated content for smooth transition
        delay(600) // Allow static content to be visible first
        showAnimatedContent = true
    }

    // Handle splash finish logic based on loading state and minimum time
    LaunchedEffect(uiState) {
        when (uiState) {
            is MainUiState.Success -> {
                // Data is loaded, transition immediately to main screen
                delay(200) // Just a tiny delay for smooth animation
                onSplashFinished()
            }
            is MainUiState.Error -> {
                // On error, also transition to main screen (it will show error there)
                delay(200)
                onSplashFinished()
            }
            MainUiState.Loading -> {
                // Keep loading, will be handled by success/error cases above
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

            // Loading text - only show during loading state
            if (uiState is MainUiState.Loading) {
                androidx.compose.material3.Text(
                    text = "Preparing prayer times...",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.alpha(logoAlpha),
                )
            }
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
