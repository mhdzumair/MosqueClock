package com.mosque.prayerclock.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mosque.prayerclock.BuildConfig
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.ui.components.AppPickerDialog
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.PrayerZones
import com.mosque.prayerclock.data.model.SoundType
import com.mosque.prayerclock.data.model.WeatherProvider
import com.mosque.prayerclock.utils.PermissionsHelper
import com.mosque.prayerclock.data.service.ApkDownloader
import com.mosque.prayerclock.data.service.UpdateChecker
import com.mosque.prayerclock.data.service.UpdateInfo
import com.mosque.prayerclock.ui.components.MarkdownText
import com.mosque.prayerclock.ui.theme.AppColorThemes
import com.mosque.prayerclock.utils.LauncherHelper
import com.mosque.prayerclock.viewmodel.CacheStats
import com.mosque.prayerclock.viewmodel.PrefetchState
import com.mosque.prayerclock.viewmodel.SettingsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Handle back button to navigate back to main screen, not exit app
    BackHandler { onNavigateBack() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface,
                            ),
                    ),
                ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
        ) {
            SettingsHeader(onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SystemSettingsAccess()
                }

                item {
                    MosqueNameSetting(
                        mosqueName = settings.mosqueName,
                        onMosqueNameChange = viewModel::updateMosqueName,
                    )
                }

                item {
                    LanguageSetting(
                        selectedLanguage = settings.language,
                        onLanguageChange = viewModel::updateLanguage,
                    )
                }

                item {
                    PrayerServiceSettings(
                        selectedServiceType = settings.prayerServiceType,
                        selectedZone = settings.selectedZone,
                        selectedRegion = settings.selectedRegion,
                        backendUrl = settings.mosqueClockBackendUrl,
                        backendApiKey = settings.mosqueClockBackendApiKey,
                        onServiceTypeChange = viewModel::updatePrayerServiceType,
                        onZoneChange = viewModel::updateSelectedZone,
                        onRegionChange = viewModel::updateSelectedRegion,
                        onBackendUrlChange = viewModel::updateMosqueClockBackendUrl,
                        onBackendApiKeyChange = viewModel::updateMosqueClockBackendApiKey,
                    )
                }

                // Show offline data download for ACJU Direct scraping
                if (settings.prayerServiceType == PrayerServiceType.ACJU_DIRECT) {
                    item {
                        OfflineDataSettings(
                            zone = settings.selectedZone,
                            viewModel = viewModel,
                        )
                    }
                }

                if (settings.prayerServiceType == PrayerServiceType.MANUAL) {
                    item {
                        ManualPrayerTimesSettings(
                            settings = settings,
                            onUpdateManualTime = viewModel::updateManualTime,
                        )
                    }
                }

                // Place Iqamah Gaps and Prayer Time setup right after Prayer Service
                item {
                    IqamahGapSettings(
                        settings = settings,
                        onUpdateIqamahGap = viewModel::updateIqamahGap,
                    )
                }

                item {
                    JummaNightBayanSettings(
                        jummaNightBayanEnabled = settings.jummaNightBayanEnabled,
                        jummaNightBayanMinutes = settings.jummaNightBayanMinutes,
                        onJummaNightBayanEnabledChange = viewModel::updateJummaNightBayanEnabled,
                        onJummaNightBayanMinutesChange = viewModel::updateJummaNightBayanMinutes,
                    )
                }

                item {
                    PrayerDisplaySettings(
                        jummahDurationMinutes = settings.jummahDurationMinutes,
                        duaDisplayDurationMinutes = settings.duaDisplayDurationMinutes,
                        showJummahScreen = settings.showJummahScreen,
                        onJummahDurationChange = viewModel::updateJummahDurationMinutes,
                        onDuaDisplayDurationChange = viewModel::updateDuaDisplayDurationMinutes,
                        onShowJummahScreenChange = viewModel::updateShowJummahScreen,
                    )
                }

                item {
                    WeatherSettings(
                        showWeather = settings.showWeather,
                        onToggleShowWeather = viewModel::updateShowWeather,
                        weatherCity = settings.weatherCity,
                        onWeatherCityChange = viewModel::updateWeatherCity,
                        provider = settings.weatherProvider,
                        onProviderChange = viewModel::updateWeatherProvider,
                        weatherApiKey = settings.weatherApiKey,
                        openWeatherMapApiKey = settings.openWeatherMapApiKey,
                        onWeatherApiKeyChange = viewModel::updateWeatherApiKey,
                        onOpenWeatherMapApiKeyChange = viewModel::updateOpenWeatherMapApiKey,
                    )
                }

                // Hijri Date Settings
                item {
                    HijriDateSettings(
                        hijriProvider = settings.hijriProvider,
                        day = settings.manualHijriDay,
                        month = settings.manualHijriMonth,
                        year = settings.manualHijriYear,
                        onHijriProviderChange = { viewModel.updateHijriProvider(it) },
                        onManualChange = { d, m, y -> viewModel.updateHijriDate(d, m, y) },
                    )
                }

                item {
                    ClockTypeSetting(
                        selectedClockType = settings.clockType,
                        onClockTypeChange = viewModel::updateClockType,
                    )
                }

                item {
                    ClockFormatSettings(
                        showSeconds = settings.showSeconds,
                        show24Hour = settings.show24HourFormat,
                        onShowSecondsChange = viewModel::updateShowSeconds,
                        onShow24HourChange = viewModel::updateShow24HourFormat,
                    )
                }

                item {
                    ColorThemeSettings(
                        selectedThemeId = settings.colorTheme,
                        onThemeChange = viewModel::updateColorTheme,
                    )
                }

                // Enhanced Azan Sound Settings
                item {
                    AzanSoundSettings(
                        azanSoundEnabled = settings.azanSoundEnabled,
                        azanSoundType = settings.azanSoundType,
                        azanSoundUri = settings.azanSoundUri,
                        onAzanSoundEnabledChange = viewModel::updateAzanSoundEnabled,
                        onAzanSoundTypeChange = viewModel::updateAzanSoundType,
                        onAzanSoundUriChange = viewModel::updateAzanSoundUri,
                    )
                }

                // Enhanced Iqamah Sound Settings
                item {
                    IqamahSoundSettings(
                        iqamahSoundEnabled = settings.iqamahSoundEnabled,
                        iqamahSoundType = settings.iqamahSoundType,
                        iqamahSoundUri = settings.iqamahSoundUri,
                        onIqamahSoundEnabledChange = viewModel::updateIqamahSoundEnabled,
                        onIqamahSoundTypeChange = viewModel::updateIqamahSoundType,
                        onIqamahSoundUriChange = viewModel::updateIqamahSoundUri,
                    )
                }

                item {
                    FullScreenCountdownSettings(
                        fullScreenCountdownEnabled = settings.fullScreenCountdownEnabled,
                        onFullScreenCountdownEnabledChange = viewModel::updateFullScreenCountdownEnabled,
                    )
                }

                item {
                    DataManagementSettings(
                        viewModel = viewModel,
                    )
                }

                item {
                    PermissionsStatus()
                }

                item {
                    AboutSettings(
                        autoUpdateCheckEnabled = settings.autoUpdateCheckEnabled,
                        onAutoUpdateCheckEnabledChange = viewModel::updateAutoUpdateCheckEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun LongPressIconButton(
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    stepDelay: Long = 100L, // Repeat interval in milliseconds
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val longPressListener by rememberUpdatedState(onLongPress)

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(500) // Initial delay before starting repeat (long press threshold)
            while (isPressed) {
                longPressListener()
                delay(stepDelay.coerceIn(1L, Long.MAX_VALUE))
            }
        }
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        interactionSource = interactionSource,
    ) {
        content()
    }
}

@Composable
private fun SettingsHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onNavigateBack,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
        ) { Text(stringResource(R.string.back)) }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = stringResource(R.string.settings),
            style =
                MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MosqueNameSetting(
    mosqueName: String,
    onMosqueNameChange: (String) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(
                    text = "🕌",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = stringResource(R.string.mosque_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            ImprovedTextField(
                value = mosqueName,
                onValueChange = onMosqueNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Enter mosque name",
                keyboardType = KeyboardType.Text,
            )
        }
    }
}

@Composable
private fun LanguageSetting(
    selectedLanguage: Language,
    onLanguageChange: (Language) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Text(
                    text = "🌐",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Language.values().forEach { language ->
                SelectableSettingsRow(
                    selected = selectedLanguage == language,
                    onClick = { onLanguageChange(language) },
                    text = language.displayName,
                )
            }
        }
    }
}

@Composable
private fun PrayerServiceSettings(
    selectedServiceType: PrayerServiceType,
    selectedZone: Int,
    selectedRegion: String,
    backendUrl: String,
    backendApiKey: String,
    onServiceTypeChange: (PrayerServiceType) -> Unit,
    onZoneChange: (Int) -> Unit,
    onRegionChange: (String) -> Unit,
    onBackendUrlChange: (String) -> Unit,
    onBackendApiKeyChange: (String) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                Text(
                    text = "📡",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = "Prayer Service",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = "Choose between our backend or third-party service for prayer times",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Service Type Selection (Manual handled separately below)
            val serviceOptions = PrayerServiceType.values().toList()
            serviceOptions.forEach { serviceType ->
                SelectableSettingsRow(
                    selected = selectedServiceType == serviceType,
                    onClick = { onServiceTypeChange(serviceType) },
                    text = serviceType.displayName,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Backend configuration for MosqueClock API
            if (selectedServiceType == PrayerServiceType.MOSQUE_CLOCK_API) {
                Text(
                    text = "Backend Configuration",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.backend_url_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Example: http://192.168.1.100:8000/ or https://api.yourdomain.com/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                ImprovedTextField(
                    value = backendUrl,
                    onValueChange = onBackendUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.backend_url_hint),
                    keyboardType = KeyboardType.Uri,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.backend_api_key_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Your backend authentication key",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                ImprovedTextField(
                    value = backendApiKey,
                    onValueChange = onBackendApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.backend_api_key_hint),
                    keyboardType = KeyboardType.Text,
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Zone selection for MosqueClock backend and ACJU Direct (both use Sri Lankan zones)
            if (selectedServiceType == PrayerServiceType.MOSQUE_CLOCK_API ||
                selectedServiceType == PrayerServiceType.ACJU_DIRECT
            ) {
                ZoneSelection(
                    selectedZone = selectedZone,
                    onZoneChange = onZoneChange,
                )
            }

            // Region selection for third-party (Al-Adhan)
            if (selectedServiceType == PrayerServiceType.AL_ADHAN_API) {
                Spacer(modifier = Modifier.height(8.dp))
                RegionSelection(
                    selectedRegion = selectedRegion,
                    onRegionChange = onRegionChange,
                )
            }
        }
    }
}

@Composable
private fun ZoneSelection(
    selectedZone: Int,
    onZoneChange: (Int) -> Unit,
) {
    var showZoneDropdown by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Sri Lankan Zone",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { showZoneDropdown = true },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val selectedZoneData = PrayerZones.zones.find { it.id == selectedZone }
                    Text(
                        text =
                            selectedZoneData?.let { "${it.name}: ${it.description}" }
                                ?: "Select Zone",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text("▼", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            DropdownMenu(
                expanded = showZoneDropdown,
                onDismissRequest = { showZoneDropdown = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                PrayerZones.zones.forEach { zone ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = zone.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = zone.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.7f,
                                        ),
                                )
                            }
                        },
                        onClick = {
                            onZoneChange(zone.id)
                            showZoneDropdown = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionSelection(
    selectedRegion: String,
    onRegionChange: (String) -> Unit,
) {
    val thirdPartyRegions =
        remember {
            listOf(
                "Colombo",
                "Kandy",
                "Galle",
                "Jaffna",
                "Kuala Lumpur",
                "Penang",
                "Singapore",
                "Jakarta",
                "Chennai",
                "Mumbai",
                "Delhi",
                "Dubai",
                "Riyadh",
                "Doha",
                "London",
                "New York",
                "Toronto",
            )
        }

    var showRegionDropdown by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "City/Region",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { showRegionDropdown = true },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            if (selectedRegion.isNotEmpty()) {
                                selectedRegion
                            } else {
                                "Select Region"
                            },
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text("▼", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            DropdownMenu(
                expanded = showRegionDropdown,
                onDismissRequest = { showRegionDropdown = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                thirdPartyRegions.forEach { region ->
                    DropdownMenuItem(
                        text = { Text(region) },
                        onClick = {
                            onRegionChange(region)
                            showRegionDropdown = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationSettings(
    city: String,
    country: String,
    onCityChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
) {
    val cities =
        remember {
            listOf(
                "Colombo" to "Sri Lanka",
                "Kandy" to "Sri Lanka",
                "Galle" to "Sri Lanka",
                "Jaffna" to "Sri Lanka",
                "Kuala Lumpur" to "Malaysia",
                "Penang" to "Malaysia",
                "Johor Bahru" to "Malaysia",
                "Singapore" to "Singapore",
                "Jakarta" to "Indonesia",
                "Bandung" to "Indonesia",
                "Surabaya" to "Indonesia",
                "Chennai" to "India",
                "Mumbai" to "India",
                "Delhi" to "India",
                "Bangalore" to "India",
                "Hyderabad" to "India",
                "Dubai" to "UAE",
                "Abu Dhabi" to "UAE",
                "Riyadh" to "Saudi Arabia",
                "Jeddah" to "Saudi Arabia",
                "Mecca" to "Saudi Arabia",
                "Medina" to "Saudi Arabia",
                "Doha" to "Qatar",
                "Kuwait City" to "Kuwait",
                "London" to "UK",
                "Manchester" to "UK",
                "Birmingham" to "UK",
                "Toronto" to "Canada",
                "Vancouver" to "Canada",
                "New York" to "USA",
                "Los Angeles" to "USA",
                "Chicago" to "USA",
                "Sydney" to "Australia",
                "Melbourne" to "Australia",
                "Perth" to "Australia",
            )
        }

    var showCityDropdown by remember { mutableStateOf(false) }

    SettingsCard {
        Column {
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // City Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showCityDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (city.isNotEmpty()) "$city, $country" else "Select City",
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text("▼", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                DropdownMenu(
                    expanded = showCityDropdown,
                    onDismissRequest = { showCityDropdown = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    cities.forEach { (cityName, countryName) ->
                        DropdownMenuItem(
                            text = { Text("$cityName, $countryName") },
                            onClick = {
                                onCityChange(cityName)
                                onCountryChange(countryName)
                                showCityDropdown = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherSettings(
    showWeather: Boolean,
    onToggleShowWeather: (Boolean) -> Unit,
    weatherCity: String,
    onWeatherCityChange: (String) -> Unit,
    provider: WeatherProvider,
    onProviderChange: (WeatherProvider) -> Unit,
    weatherApiKey: String,
    openWeatherMapApiKey: String,
    onWeatherApiKeyChange: (String) -> Unit,
    onOpenWeatherMapApiKeyChange: (String) -> Unit,
) {
    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "🌤️",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.weather),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.weather_city_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
                Switch(checked = showWeather, onCheckedChange = onToggleShowWeather)
            }
            if (showWeather) {
                // Provider selection (radio)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Provider",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    WeatherProvider.values().forEach { p ->
                        SelectableSettingsRow(
                            selected = provider == p,
                            onClick = { onProviderChange(p) },
                            text = p.displayName,
                        )
                    }
                }

                // Show API key field for WeatherAPI.com
                if (provider == WeatherProvider.WEATHER_API) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.weather_api_key_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Get free key from weatherapi.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ImprovedTextField(
                        value = weatherApiKey,
                        onValueChange = onWeatherApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = stringResource(R.string.weather_api_key_hint),
                        keyboardType = KeyboardType.Text,
                    )
                }

                // Show API key field for OpenWeatherMap
                if (provider == WeatherProvider.OPEN_WEATHER_MAP) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.openweathermap_api_key_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Get free key from openweathermap.org/api",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ImprovedTextField(
                        value = openWeatherMapApiKey,
                        onValueChange = onOpenWeatherMapApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = stringResource(R.string.openweathermap_api_key_hint),
                        keyboardType = KeyboardType.Text,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.weather_city),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                ImprovedTextField(
                    value = weatherCity,
                    onValueChange = onWeatherCityChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.weather_placeholder),
                    keyboardType = KeyboardType.Text,
                )
            }
        }
    }
}

@Composable
private fun ManualPrayerTimesSettings(
    settings: AppSettings,
    onUpdateManualTime: (String, String) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Text(
                    text = "⏱️",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = "Prayer Times Setup",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            PrayerTimeInput(
                label = "Fajr Azan",
                time = settings.manualFajrAzan,
                onTimeChange = { onUpdateManualTime("fajrAzan", it) },
            )

            PrayerTimeInput(
                label = "Sunrise",
                time = settings.manualSunrise,
                onTimeChange = { onUpdateManualTime("sunrise", it) },
            )

            PrayerTimeInput(
                label = "Dhuhr Azan",
                time = settings.manualDhuhrAzan,
                onTimeChange = { onUpdateManualTime("dhuhrAzan", it) },
            )

            PrayerTimeInput(
                label = "Asr Azan",
                time = settings.manualAsrAzan,
                onTimeChange = { onUpdateManualTime("asrAzan", it) },
            )

            PrayerTimeInput(
                label = "Maghrib Azan",
                time = settings.manualMaghribAzan,
                onTimeChange = { onUpdateManualTime("maghribAzan", it) },
            )

            PrayerTimeInput(
                label = "Isha Azan",
                time = settings.manualIshaAzan,
                onTimeChange = { onUpdateManualTime("ishaAzan", it) },
            )
        }
    }
}

@Composable
private fun IqamahGapSettings(
    settings: AppSettings,
    onUpdateIqamahGap: (String, Int) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Text(
                    text = "⏳",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = "Iqamah Time Gaps (minutes after Azan)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            IqamahGapInput(
                label = "Fajr",
                gap = settings.fajrIqamahGap,
                onGapChange = { onUpdateIqamahGap("fajr", it) },
            )

            IqamahGapInput(
                label = "Dhuhr",
                gap = settings.dhuhrIqamahGap,
                onGapChange = { onUpdateIqamahGap("dhuhr", it) },
            )

            IqamahGapInput(
                label = "Asr",
                gap = settings.asrIqamahGap,
                onGapChange = { onUpdateIqamahGap("asr", it) },
            )

            IqamahGapInput(
                label = "Maghrib",
                gap = settings.maghribIqamahGap,
                onGapChange = { onUpdateIqamahGap("maghrib", it) },
            )

            IqamahGapInput(
                label = "Isha",
                gap = settings.ishaIqamahGap,
                onGapChange = { onUpdateIqamahGap("isha", it) },
            )
        }
    }
}

@Composable
private fun JummaNightBayanSettings(
    jummaNightBayanEnabled: Boolean,
    jummaNightBayanMinutes: Int,
    onJummaNightBayanEnabledChange: (Boolean) -> Unit,
    onJummaNightBayanMinutesChange: (Int) -> Unit,
) {
    SettingsCard {
        Column {
            // Header with toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "🌙",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column {
                        Text(
                            text = "Jumma Night Bayan",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Special timing for Thursday night",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
                Switch(
                    checked = jummaNightBayanEnabled,
                    onCheckedChange = onJummaNightBayanEnabledChange,
                )
            }

            // Description
            if (jummaNightBayanEnabled) {
                Text(
                    text = "When enabled, Thursday night Isha Iqamah will be delayed by the specified duration to accommodate the Jumma Night Bayan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                // Duration input
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Bayan Duration",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    NumberPickerField(
                        value = jummaNightBayanMinutes,
                        onValueChange = onJummaNightBayanMinutesChange,
                        modifier = Modifier.width(160.dp),
                        range = 15..90,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerDisplaySettings(
    jummahDurationMinutes: Int,
    duaDisplayDurationMinutes: Int,
    showJummahScreen: Boolean,
    onJummahDurationChange: (Int) -> Unit,
    onDuaDisplayDurationChange: (Int) -> Unit,
    onShowJummahScreenChange: (Boolean) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Text(
                    text = "⏰",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = "Prayer Display Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = "How long to show Jumu'ah (Friday) prayer as active after Azan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Jumu'ah Duration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )

                NumberPickerField(
                    value = jummahDurationMinutes,
                    onValueChange = onJummahDurationChange,
                    modifier = Modifier.width(160.dp),
                    range = 30..120,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show Jummah Screen toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show Jumu'ah Screen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Display full-screen countdown during Jumu'ah prayer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Switch(
                    checked = showJummahScreen,
                    onCheckedChange = onShowJummahScreenChange,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "How long to show 'Dua for Joining Saff' after Iqamah time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Dua Duration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )

                NumberPickerField(
                    value = duaDisplayDurationMinutes,
                    onValueChange = onDuaDisplayDurationChange,
                    modifier = Modifier.width(160.dp),
                    range = 1..15,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun PrayerTimeInput(
    label: String,
    time: String,
    onTimeChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        TimePickerField(
            value = time,
            onValueChange = onTimeChange,
            modifier = Modifier.width(200.dp),
        )
    }
}

@Composable
private fun IqamahGapInput(
    label: String,
    gap: Int,
    onGapChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        NumberPickerField(
            value = gap,
            onValueChange = onGapChange,
            modifier = Modifier.width(160.dp),
            range = 0..60,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "min",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun ClockTypeSetting(
    selectedClockType: ClockType,
    onClockTypeChange: (ClockType) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Text(
                    text = "🕐",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = stringResource(R.string.clock_type),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            ClockType.values().forEach { clockType ->
                SelectableSettingsRow(
                    selected = selectedClockType == clockType,
                    onClick = { onClockTypeChange(clockType) },
                    text =
                        when (clockType) {
                            ClockType.DIGITAL -> stringResource(R.string.digital_clock)
                            ClockType.ANALOG -> stringResource(R.string.analog_clock)
                            ClockType.BOTH -> "Both (Auto Cycle)"
                        },
                )
            }
        }
    }
}

@Composable
private fun ClockFormatSettings(
    showSeconds: Boolean,
    show24Hour: Boolean,
    onShowSecondsChange: (Boolean) -> Unit,
    onShow24HourChange: (Boolean) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Text(
                    text = "🔢",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = "Clock Format",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.show_seconds),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Switch(
                    checked = showSeconds,
                    onCheckedChange = onShowSecondsChange,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.hour_format_24),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Switch(
                    checked = show24Hour,
                    onCheckedChange = onShow24HourChange,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    isSelected: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
        ) { content() }
    }
}

@Composable
private fun HijriDateSettings(
    hijriProvider: com.mosque.prayerclock.data.model.HijriProvider,
    day: Int,
    month: Int,
    year: Int,
    onHijriProviderChange: (com.mosque.prayerclock.data.model.HijriProvider) -> Unit,
    onManualChange: (Int, Int, Int) -> Unit,
) {
    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "🌙",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = "Hijri Date Source",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Hijri Provider Selection
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                com.mosque.prayerclock.data.model.HijriProvider.values().forEach { provider ->
                    SelectableSettingsRow(
                        selected = hijriProvider == provider,
                        onClick = { onHijriProviderChange(provider) },
                        text = provider.displayName,
                    )
                }
            }

            if (hijriProvider == com.mosque.prayerclock.data.model.HijriProvider.MANUAL) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NumberPickerField(
                        value = day,
                        onValueChange = { onManualChange(it, month, year) },
                        range = 1..30,
                    )
                    NumberPickerField(
                        value = month,
                        onValueChange = { onManualChange(day, it, year) },
                        range = 1..12,
                    )
                    NumberPickerField(
                        value = year,
                        onValueChange = { onManualChange(day, month, it) },
                        range = 1300..1600,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImprovedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var showDialog by remember { mutableStateOf(false) }

    // Clickable card that displays current value
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable { showDialog = true },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = value.ifEmpty { placeholder },
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                        if (value.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }

            // Edit icon indicator
            Text(
                text = "✏️",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }

    // Show dialog when clicked
    if (showDialog) {
        TextInputDialog(
            title = label ?: "Edit",
            initialValue = value,
            placeholder = placeholder,
            keyboardType = keyboardType,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                onValueChange(newValue)
                showDialog = false
            },
        )
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    initialValue: String,
    placeholder: String,
    keyboardType: KeyboardType,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var textValue by remember { mutableStateOf(initialValue) }
    val focusManager = LocalFocusManager.current

    // Handle back button to dismiss dialog
    BackHandler { onDismiss() }

    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val availableWidth = maxWidth
            val availableHeight = maxHeight

            // Calculate dynamic sizes based on screen dimensions
            val dialogWidth = (availableWidth * 0.85f).coerceAtMost(600.dp)

            val dialogPadding = (availableWidth * 0.04f).coerceIn(16.dp, 32.dp)

            val titleFontSize =
                with(density) {
                    val sp = (availableWidth.toPx() * 0.025f).toSp()
                    sp.value.coerceIn(18f, 28f).sp
                }

            val textFieldFontSize =
                with(density) {
                    val sp = (availableWidth.toPx() * 0.022f).toSp()
                    sp.value.coerceIn(16f, 24f).sp
                }

            val buttonFontSize =
                with(density) {
                    val sp = (availableWidth.toPx() * 0.02f).toSp()
                    sp.value.coerceIn(14f, 22f).sp
                }

            val verticalSpacing = (availableHeight * 0.02f).coerceIn(12.dp, 24.dp)

            Card(
                modifier =
                    Modifier
                        .width(dialogWidth)
                        .padding(dialogPadding),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(dialogPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontSize = titleFontSize,
                            ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(verticalSpacing))

                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(placeholder)
                        },
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = keyboardType,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onConfirm(textValue)
                                },
                            ),
                        singleLine = true,
                        textStyle =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontSize = textFieldFontSize,
                            ),
                    )

                    Spacer(modifier = Modifier.height(verticalSpacing * 1.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val buttonHeight = (availableHeight * 0.06f).coerceIn(48.dp, 64.dp)

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = buttonHeight),
                        ) {
                            Text(
                                text = "Cancel",
                                style =
                                    MaterialTheme.typography.labelLarge.copy(
                                        fontSize = buttonFontSize,
                                    ),
                            )
                        }

                        Button(
                            onClick = { onConfirm(textValue) },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = buttonHeight),
                        ) {
                            Text(
                                text = "Save",
                                style =
                                    MaterialTheme.typography.labelLarge.copy(
                                        fontSize = buttonFontSize,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hours by remember { mutableStateOf("00") }
    var minutes by remember { mutableStateOf("00") }
    var isInitialized by remember { mutableStateOf(false) }

    // Parse initial value
    LaunchedEffect(value) {
        if (value.isNotEmpty()) {
            val parts = value.split(":")
            if (parts.size >= 2) {
                hours = parts[0].padStart(2, '0')
                minutes = parts[1].padStart(2, '0')
            } else {
                // Handle cases where value might be malformed
                hours = "00"
                minutes = "00"
            }
        } else {
            hours = "00"
            minutes = "00"
        }
        isInitialized = true
    }

    // Update value when hours or minutes change (only after initialization)
    LaunchedEffect(hours, minutes, isInitialized) {
        if (isInitialized) {
            val h = hours.toIntOrNull()?.coerceIn(0, 23) ?: 0
            val m = minutes.toIntOrNull()?.coerceIn(0, 59) ?: 0
            onValueChange(String.format("%02d:%02d", h, m))
        }
    }

    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hours picker
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LongPressIconButton(
                    onClick = {
                        val currentHour = hours.toIntOrNull() ?: 0
                        val newHour = (currentHour + 1) % 24
                        hours = String.format("%02d", newHour)
                    },
                    onLongPress = {
                        val currentHour = hours.toIntOrNull() ?: 0
                        val newHour = (currentHour + 1) % 24
                        hours = String.format("%02d", newHour)
                    },
                ) {
                    Text(
                        text = "+",
                        color = MaterialTheme.colorScheme.primary,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = hours.padStart(2, '0'),
                        style =
                            MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                LongPressIconButton(
                    onClick = {
                        val currentHour = hours.toIntOrNull() ?: 0
                        val newHour = if (currentHour <= 0) 23 else currentHour - 1
                        hours = String.format("%02d", newHour)
                    },
                    onLongPress = {
                        val currentHour = hours.toIntOrNull() ?: 0
                        val newHour = if (currentHour <= 0) 23 else currentHour - 1
                        hours = String.format("%02d", newHour)
                    },
                ) {
                    Text(
                        text = "−",
                        color = MaterialTheme.colorScheme.primary,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }

                Text(
                    text = "HH",
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }

            Text(
                text = ":",
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.primary,
            )

            // Minutes picker
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LongPressIconButton(
                    onClick = {
                        val currentMin = minutes.toIntOrNull() ?: 0
                        val newMin = (currentMin + 1) % 60
                        minutes = String.format("%02d", newMin)
                    },
                    onLongPress = {
                        val currentMin = minutes.toIntOrNull() ?: 0
                        val newMin = (currentMin + 1) % 60
                        minutes = String.format("%02d", newMin)
                    },
                ) {
                    Text(
                        text = "+",
                        color = MaterialTheme.colorScheme.primary,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = minutes.padStart(2, '0'),
                        style =
                            MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                LongPressIconButton(
                    onClick = {
                        val currentMin = minutes.toIntOrNull() ?: 0
                        val newMin = if (currentMin <= 0) 59 else currentMin - 1
                        minutes = String.format("%02d", newMin)
                    },
                    onLongPress = {
                        val currentMin = minutes.toIntOrNull() ?: 0
                        val newMin = if (currentMin <= 0) 59 else currentMin - 1
                        minutes = String.format("%02d", newMin)
                    },
                ) {
                    Text(
                        text = "−",
                        color = MaterialTheme.colorScheme.primary,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }

                Text(
                    text = "MM",
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun NumberPickerField(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 0..99,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Decrease button
        LongPressIconButton(
            onClick = {
                val newValue = (value - 1).coerceIn(range)
                onValueChange(newValue)
            },
            onLongPress = {
                val newValue = (value - 1).coerceIn(range)
                onValueChange(newValue)
            },
            enabled = value > range.first,
        ) {
            Text(
                text = "−",
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color =
                    if (value > range.first) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
            )
        }

        // Number display (card-based for TV/remote friendliness)
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Box(
                modifier = Modifier.width(64.dp).padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value.toString().padStart(2, '0'),
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Increase button
        LongPressIconButton(
            onClick = {
                val newValue = (value + 1).coerceIn(range)
                onValueChange(newValue)
            },
            onLongPress = {
                val newValue = (value + 1).coerceIn(range)
                onValueChange(newValue)
            },
            enabled = value < range.last,
        ) {
            Text(
                text = "+",
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color =
                    if (value < range.last) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
            )
        }
    }
}

@Composable
private fun SelectableSettingsRow(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = onClick,
                ).background(
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.2f,
                            ) // Bright highlight for selected
                        } else {
                            Color.Transparent
                        },
                    shape = RoundedCornerShape(8.dp),
                ).border(
                    width = if (selected) 2.dp else 0.dp,
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme
                                .primary // Bright border for selected
                        } else {
                            Color.Transparent
                        },
                    shape = RoundedCornerShape(8.dp),
                ).padding(
                    horizontal = 12.dp,
                    vertical = 12.dp,
                ), // More padding for better touch target
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor =
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                ),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary // Highlight selected text
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun FullScreenCountdownSettings(
    fullScreenCountdownEnabled: Boolean,
    onFullScreenCountdownEnabledChange: (Boolean) -> Unit,
) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "⏱️",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.fullscreen_countdown_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.fullscreen_countdown_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
            Switch(checked = fullScreenCountdownEnabled, onCheckedChange = onFullScreenCountdownEnabledChange)
        }
    }
}

@Composable
private fun ColorThemeSettings(
    selectedThemeId: String,
    onThemeChange: (String) -> Unit,
) {
    val allThemes = AppColorThemes.getAllThemes()
    var isExpanded by remember { mutableStateOf(false) }

    SettingsCard {
        Column {
            // Header with icon and title
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "🎨",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.color_theme_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.color_theme_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )

                        // Show currently selected theme name
                        val currentTheme = allThemes.find { it.id == selectedThemeId }
                        currentTheme?.let {
                            Text(
                                text = "Current: ${it.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Expandable theme grid
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Display themes in a vertical list for TV UI
                    allThemes.forEach { theme ->
                        ThemeOption(
                            theme = theme,
                            isSelected = theme.id == selectedThemeId,
                            onSelect = { onThemeChange(theme.id) },
                        )
                        if (theme != allThemes.last()) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    theme: com.mosque.prayerclock.ui.theme.ColorTheme,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onSelect() },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        border =
            if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme.name,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Color preview squares
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 12.dp),
            ) {
                // Show 3 key colors as preview
                ColorPreviewBox(color = theme.primaryAccent)
                ColorPreviewBox(color = theme.surfacePrimary)
                ColorPreviewBox(color = theme.azanTime)
            }
        }
    }
}

@Composable
private fun ColorPreviewBox(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .background(color, shape = RoundedCornerShape(4.dp))
                .border(
                    width = 1.dp,
                    color =
                        androidx.compose.ui.graphics.Color.White
                            .copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp),
                ),
    )
}

@Composable
private fun PermissionsStatus() {
    val context = LocalContext.current
    var permissionsList by remember { mutableStateOf<List<com.mosque.prayerclock.utils.PermissionsHelper.PermissionStatus>>(emptyList()) }
    var isExpanded by remember { mutableStateOf(false) }

    // Permission launcher for runtime permissions
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            // Refresh permissions list after permission result
            permissionsList = com.mosque.prayerclock.utils.PermissionsHelper.getAllPermissionStatuses(context)
            if (isGranted) {
                Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // Overlay permission launcher
    val overlayPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            // Refresh permissions list after returning from settings
            permissionsList = com.mosque.prayerclock.utils.PermissionsHelper.getAllPermissionStatuses(context)
        }

    // Install packages permission launcher
    val installPackagesLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            // Refresh permissions list after returning from settings
            permissionsList = com.mosque.prayerclock.utils.PermissionsHelper.getAllPermissionStatuses(context)
        }

    // Default launcher permission launcher
    val defaultLauncherLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            // Refresh permissions list after returning from settings
            permissionsList = com.mosque.prayerclock.utils.PermissionsHelper.getAllPermissionStatuses(context)
        }

    // Battery optimization launcher
    val batteryOptimizationLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            // Refresh permissions list after returning from settings
            permissionsList = com.mosque.prayerclock.utils.PermissionsHelper.getAllPermissionStatuses(context)
        }

    // Load permissions on first composition
    LaunchedEffect(Unit) {
        permissionsList = com.mosque.prayerclock.utils.PermissionsHelper.getAllPermissionStatuses(context)
    }

    // Refresh permissions when expanded
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            permissionsList = com.mosque.prayerclock.utils.PermissionsHelper.getAllPermissionStatuses(context)
        }
    }

    val grantedCount = permissionsList.count { it.isGranted }
    val totalCount = permissionsList.size
    val allGranted = grantedCount == totalCount

    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header with expand/collapse
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (allGranted) "✅" else "⚠️",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "$grantedCount of $totalCount permissions granted",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (allGranted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            },
                    )
                }
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            // Expandable permission list
            if (isExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                
                permissionsList.forEach { permission ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (permission.isGranted) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                    },
                            ),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Only allow clicking if permission is not granted
                                        if (!permission.isGranted) {
                                            when (permission.permissionType) {
                                                com.mosque.prayerclock.utils.PermissionsHelper.PermissionType.RUNTIME -> {
                                                    // Request runtime permission directly
                                                    permission.permissionName?.let {
                                                        permissionLauncher.launch(it)
                                                    }
                                                }
                                                com.mosque.prayerclock.utils.PermissionsHelper.PermissionType.OVERLAY -> {
                                                    // Request overlay permission
                                                    permission.settingsIntent?.let {
                                                        overlayPermissionLauncher.launch(it)
                                                    }
                                                }
                                                com.mosque.prayerclock.utils.PermissionsHelper.PermissionType.INSTALL_PACKAGES -> {
                                                    // Request install packages permission
                                                    permission.settingsIntent?.let {
                                                        installPackagesLauncher.launch(it)
                                                    }
                                                }
                                                com.mosque.prayerclock.utils.PermissionsHelper.PermissionType.DEFAULT_LAUNCHER -> {
                                                    // Request default launcher
                                                    permission.settingsIntent?.let {
                                                        defaultLauncherLauncher.launch(it)
                                                    }
                                                }
                                                com.mosque.prayerclock.utils.PermissionsHelper.PermissionType.BATTERY_OPTIMIZATION -> {
                                                    // Request battery optimization
                                                    permission.settingsIntent?.let {
                                                        batteryOptimizationLauncher.launch(it)
                                                    }
                                                }
                                                com.mosque.prayerclock.utils.PermissionsHelper.PermissionType.SYSTEM_SETTINGS -> {
                                                    // Open system settings
                                                    permission.settingsIntent?.let {
                                                        com.mosque.prayerclock.utils.PermissionsHelper.openPermissionSettings(
                                                            context,
                                                            it,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (permission.isGranted) "✓" else if (permission.isRequired) "✗" else "○",
                                        style = MaterialTheme.typography.titleMedium,
                                        color =
                                            if (permission.isGranted) {
                                                MaterialTheme.colorScheme.primary
                                            } else if (permission.isRequired) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            },
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                    Text(
                                        text = permission.name,
                                        style =
                                            MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (permission.isRequired) {
                                        Text(
                                            text = " *",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                                Text(
                                    text = permission.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                            if (!permission.isGranted) {
                                Text(
                                    text = "→",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                // Legend
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "* Required permissions    ✓ Granted    ✗ Not granted    ○ Optional",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun SystemSettingsAccess() {
    val context = LocalContext.current
    var showAppPicker by remember { mutableStateOf(false) }

    // Show app picker dialog
    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { app ->
                LauncherHelper.launchApp(context, app.resolveInfo)
            },
        )
    }

    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "⚙️",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.system_settings),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.system_settings_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Date & Time Settings Button
            OutlinedButton(
                onClick = {
                    if (!LauncherHelper.openDateTimeSettings(context)) {
                        Toast.makeText(context, "Unable to open Date & Time settings", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "⏰",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.date_time_settings),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.open_date_time_settings),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // General Android Settings Button
            OutlinedButton(
                onClick = {
                    if (!LauncherHelper.openAndroidSettings(context)) {
                        Toast.makeText(context, "Unable to open Android Settings", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "⚙️",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.android_settings),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.open_android_settings),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // App Drawer Button
            OutlinedButton(
                onClick = {
                    showAppPicker = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "📱",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.open_app_drawer),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.open_app_drawer_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Switch Launcher Button
            OutlinedButton(
                onClick = {
                    if (!LauncherHelper.openLauncherChooser(context)) {
                        Toast.makeText(context, "Unable to switch launcher", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "🏠",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.choose_launcher),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.choose_launcher_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // File Manager Button
            OutlinedButton(
                onClick = {
                    if (!LauncherHelper.openFileManager(context)) {
                        // Toast already shown in LauncherHelper
                        // No need for duplicate message
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "📁",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.open_file_manager),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.open_file_manager_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataManagementSettings(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheStats by remember { mutableStateOf<CacheStats?>(null) }
    var isClearing by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Load cache stats when component is first composed
    LaunchedEffect(Unit) {
        cacheStats = viewModel.getCacheStats()
    }

    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "🗄️",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Manage cached prayer times and hijri dates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Cache Statistics
            if (cacheStats != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Prayer Times Cached:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${cacheStats!!.prayerTimesCount} entries",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Hijri Dates Cached:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${cacheStats!!.hijriDatesCount} entries",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // Clear Cache Button
            OutlinedButton(
                onClick = {
                    showConfirmDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isClearing && (cacheStats?.let { it.prayerTimesCount + it.hijriDatesCount } ?: 0) > 0,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isClearing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                        Text("Clearing cache...")
                    } else {
                        Text("🗑️ Clear All Cached Data")
                    }
                }
            }

            Text(
                text = "⚠️ This will clear all cached prayer times and hijri dates. Data will be re-fetched from your selected sources when needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        Dialog(onDismissRequest = { showConfirmDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Text(
                            text = "Clear All Cached Data?",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Text(
                        text = "This will delete all cached prayer times and hijri dates. The app will need to fetch fresh data from your configured sources.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )

                    if (cacheStats != null) {
                        Text(
                            text = "• ${cacheStats!!.prayerTimesCount} prayer time entries\n• ${cacheStats!!.hijriDatesCount} hijri date entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showConfirmDialog = false },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                showConfirmDialog = false
                                isClearing = true
                                scope.launch {
                                    try {
                                        viewModel.clearAllCachedData()
                                        // Refresh stats
                                        cacheStats = viewModel.getCacheStats()
                                        Toast.makeText(
                                            context,
                                            "✅ Cache cleared successfully",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "❌ Failed to clear cache: ${e.message}",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    } finally {
                                        isClearing = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutSettings(
    autoUpdateCheckEnabled: Boolean = true,
    onAutoUpdateCheckEnabledChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var updateCheckError by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var isFileAlreadyDownloaded by remember { mutableStateOf(false) }
    var hasInstallPermission by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val apkDownloader = remember { ApkDownloader() }
    val downloadProgress by apkDownloader.downloadProgress.collectAsState()

    // Get current version from BuildConfig
    val currentVersion = BuildConfig.VERSION_NAME

    // Check install permission on composition
    LaunchedEffect(Unit) {
        hasInstallPermission = apkDownloader.canInstallPackages(context)
    }

    // Check if APK is already downloaded when update info changes
    LaunchedEffect(updateInfo) {
        if (updateInfo?.hasUpdate == true) {
            isFileAlreadyDownloaded =
                apkDownloader.isApkAlreadyDownloaded(
                    context = context,
                    version = updateInfo!!.latestVersion,
                )
            // Also re-check permission
            hasInstallPermission = apkDownloader.canInstallPackages(context)
        }
    }

    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ℹ️",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.about_app),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "MosqueClock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Version Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = currentVersion,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Developer Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.developer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.developer_name),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Self-update features only available for GitHub/sideload distribution
            // Play Store builds use Google Play for updates (per Google policy)
            if (BuildConfig.ENABLE_SELF_UPDATE) {
                Spacer(modifier = Modifier.height(12.dp))

                // Auto Update Check Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_update_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.auto_update_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                    Switch(
                        checked = autoUpdateCheckEnabled,
                        onCheckedChange = onAutoUpdateCheckEnabledChange,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Check for Updates Button
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isChecking = true
                            updateCheckError = false
                            try {
                                val updateChecker = UpdateChecker()
                                val result = updateChecker.checkForUpdates(currentVersion)
                                updateInfo = result
                                if (result == null) {
                                    updateCheckError = true
                                }
                            } catch (e: Exception) {
                                updateCheckError = true
                            } finally {
                                isChecking = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isChecking,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).padding(end = 8.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(stringResource(R.string.checking_updates))
                        } else {
                            Text(stringResource(R.string.check_for_updates))
                        }
                    }
                }
            }

            // Update Status (only for GitHub/sideload distribution)
            if (BuildConfig.ENABLE_SELF_UPDATE) {
                if (updateInfo != null && !isChecking) {
                    if (updateInfo!!.hasUpdate) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.update_available, updateInfo!!.latestVersion),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        Text(
                                            text = "Current: v$currentVersion",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Install permission warning (Android 8.0+)
                                if (!hasInstallPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                            CardDefaults.cardColors(
                                                containerColor =
                                                    MaterialTheme.colorScheme.errorContainer.copy(
                                                        alpha = 0.3f,
                                                    ),
                                            ),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                        ) {
                                            Text(
                                                text = "⚠️ Install Permission Required",
                                                style =
                                                    MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text =
                                                    "To install updates, you need to grant install permission for this app.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    apkDownloader.requestInstallPermission(context)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors =
                                                    ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.error,
                                                    ),
                                            ) {
                                                Text("Grant Install Permission")
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Download and Install Button
                                Button(
                                    onClick = {
                                        // Check permission first
                                        if (!hasInstallPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            apkDownloader.requestInstallPermission(context)
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Please allow installing from this source to update the app",
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                            return@Button
                                        }

                                        if (isFileAlreadyDownloaded) {
                                            // If file is already downloaded, just install it
                                            val fileName = apkDownloader.getApkFileName(updateInfo!!.latestVersion)
                                            val file = File(context.getExternalFilesDir(null), fileName)

                                            if (!file.exists()) {
                                                Toast.makeText(context, "APK file not found", Toast.LENGTH_SHORT).show()
                                                isFileAlreadyDownloaded = false
                                                return@Button
                                            }

                                            val uri: Uri =
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        file,
                                                    )
                                                } else {
                                                    Uri.fromFile(file)
                                                }

                                            // Use ACTION_INSTALL_PACKAGE for better compatibility (Android 8.0+)
                                            val installIntent =
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                                                        setDataAndType(uri, "application/vnd.android.package-archive")
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                                                        putExtra(Intent.EXTRA_RETURN_RESULT, true)
                                                    }
                                                } else {
                                                    Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, "application/vnd.android.package-archive")
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                }

                                            if (installIntent.resolveActivity(context.packageManager) != null) {
                                                context.startActivity(installIntent)
                                                Toast.makeText(context, "Opening installer...", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No installer app found", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            // Download the file
                                            isDownloading = true
                                            apkDownloader.downloadApk(
                                                context = context,
                                                downloadUrl = updateInfo!!.downloadUrl,
                                                version = updateInfo!!.latestVersion,
                                                onComplete = {
                                                    isDownloading = false
                                                    isFileAlreadyDownloaded = true
                                                },
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isDownloading,
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (isDownloading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp).padding(end = 8.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                            if (downloadProgress.progress > 0) {
                                                Text("Downloading ${downloadProgress.progress}%")
                                            } else {
                                                Text("Downloading...")
                                            }
                                        } else {
                                            Text(
                                                if (isFileAlreadyDownloaded) {
                                                    stringResource(R.string.install_update)
                                                } else {
                                                    stringResource(R.string.download_update)
                                                },
                                            )
                                        }
                                    }
                                }

                                // Download Progress Bar
                                if (isDownloading && downloadProgress.progress > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = downloadProgress.progress / 100f,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    Text(
                                        text = "${apkDownloader.formatBytes(
                                            downloadProgress.bytesDownloaded,
                                        )} / ${apkDownloader.formatBytes(downloadProgress.totalBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    )
                                }

                                // Release Notes (if available)
                                if (updateInfo!!.releaseNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "What's New:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    // Show full release notes with markdown formatting
                                    MarkdownText(
                                        markdown = updateInfo!!.releaseNotes,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.no_updates_available),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }

                if (updateCheckError && !isChecking) {
                    Text(
                        text = stringResource(R.string.update_check_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // GitHub Repository Button
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mhdzumair/MosqueClock"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "💻",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(stringResource(R.string.github_repository))
                }
            }
        }
    }
}

@Composable
private fun AzanSoundSettings(
    azanSoundEnabled: Boolean,
    azanSoundType: SoundType,
    azanSoundUri: String,
    onAzanSoundEnabledChange: (Boolean) -> Unit,
    onAzanSoundTypeChange: (SoundType) -> Unit,
    onAzanSoundUriChange: (String) -> Unit,
) {
    val context = LocalContext.current
    
    // Permission launcher for audio access
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Permission granted. Please select audio again.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permission denied. Cannot access audio files.", Toast.LENGTH_LONG).show()
            }
        }
    
    // Audio file picker launcher using OpenDocument for persistent access
    val audioPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let {
                // Take persistable URI permission for background service access
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    onAzanSoundUriChange(it.toString())
                    Log.d("AzanSoundSettings", "Persistable permission granted for URI: $it")
                    Toast.makeText(context, "Azan audio file selected", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("AzanSoundSettings", "Failed to take persistable permission", e)
                    Toast.makeText(context, "Error: Unable to access this file. Please try a different file.", Toast.LENGTH_LONG).show()
                }
            }
        }
    
    // Function to check and request audio permission
    val checkAndRequestAudioPermission: () -> Unit = {
        if (PermissionsHelper.hasAudioPermission(context)) {
            // Permission already granted, open picker
            audioPickerLauncher.launch(arrayOf("audio/*"))
        } else {
            // Request permission
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        }
    }

    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "🕌",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.azan_sound_settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.azan_sound_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
                Switch(checked = azanSoundEnabled, onCheckedChange = onAzanSoundEnabledChange)
            }

            // Sound type selection (only shown when enabled)
            if (azanSoundEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sound_type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )

                    // Sound type options
                    SoundType.values().forEach { type ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onAzanSoundTypeChange(type) }
                                    .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = azanSoundType == type,
                                onClick = { onAzanSoundTypeChange(type) },
                                colors =
                                    RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                    ),
                            )
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }

                    // Custom audio file selection
                    if (azanSoundType == SoundType.CUSTOM) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 48.dp),
                        ) {
                            OutlinedButton(
                                onClick = checkAndRequestAudioPermission,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.select_custom_audio))
                            }

                            if (azanSoundUri.isNotEmpty()) {
                                val fileName =
                                    try {
                                        getFileNameFromUri(context, Uri.parse(azanSoundUri))
                                    } catch (e: Exception) {
                                        "Unknown"
                                    }
                                
                                // Check if URI is still accessible
                                val isAccessible = try {
                                    context.contentResolver.openFileDescriptor(Uri.parse(azanSoundUri), "r")?.close()
                                    true
                                } catch (e: Exception) {
                                    false
                                }
                                
                                if (isAccessible) {
                                    Text(
                                        text = stringResource(R.string.audio_file_selected, fileName),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                } else {
                                    Column(modifier = Modifier.padding(top = 4.dp)) {
                                        Text(
                                            text = "⚠️ File not accessible: $fileName",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                        Text(
                                            text = "Please re-select the audio file",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.no_audio_selected),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper function to get the actual filename from a content URI
 */
private fun getFileNameFromUri(
    context: Context,
    uri: Uri,
): String {
    var result = "Unknown"
    
    // Try to query the content resolver for the display name
    if (uri.scheme == "content") {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = it.getString(displayNameIndex) ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error getting filename from URI", e)
        }
    }
    
    // Fallback to last path segment
    if (result == "Unknown") {
        result = uri.lastPathSegment ?: "Unknown"
    }
    
    return result
}

@Composable
private fun IqamahSoundSettings(
    iqamahSoundEnabled: Boolean,
    iqamahSoundType: SoundType,
    iqamahSoundUri: String,
    onIqamahSoundEnabledChange: (Boolean) -> Unit,
    onIqamahSoundTypeChange: (SoundType) -> Unit,
    onIqamahSoundUriChange: (String) -> Unit,
) {
    val context = LocalContext.current
    
    // Permission launcher for audio access
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Permission granted. Please select audio again.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permission denied. Cannot access audio files.", Toast.LENGTH_LONG).show()
            }
        }
    
    // Audio file picker launcher using OpenDocument for persistent access
    val audioPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let {
                // Take persistable URI permission for background service access
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    onIqamahSoundUriChange(it.toString())
                    Log.d("IqamahSoundSettings", "Persistable permission granted for URI: $it")
                    Toast.makeText(context, "Iqamah audio file selected", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("IqamahSoundSettings", "Failed to take persistable permission", e)
                    Toast.makeText(context, "Error: Unable to access this file. Please try a different file.", Toast.LENGTH_LONG).show()
                }
            }
        }
    
    // Function to check and request audio permission
    val checkAndRequestAudioPermission: () -> Unit = {
        if (PermissionsHelper.hasAudioPermission(context)) {
            // Permission already granted, open picker
            audioPickerLauncher.launch(arrayOf("audio/*"))
        } else {
            // Request permission
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        }
    }

    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "🙏",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.iqamah_sound_settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.iqamah_sound_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
                Switch(checked = iqamahSoundEnabled, onCheckedChange = onIqamahSoundEnabledChange)
            }

            // Sound type selection (only shown when enabled)
            if (iqamahSoundEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sound_type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )

                    // Sound type options
                    SoundType.values().forEach { type ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onIqamahSoundTypeChange(type) }
                                    .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = iqamahSoundType == type,
                                onClick = { onIqamahSoundTypeChange(type) },
                                colors =
                                    RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                    ),
                            )
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }

                    // Custom audio file selection
                    if (iqamahSoundType == SoundType.CUSTOM) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 48.dp),
                        ) {
                            OutlinedButton(
                                onClick = checkAndRequestAudioPermission,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.select_custom_audio))
                            }

                            if (iqamahSoundUri.isNotEmpty()) {
                                val fileName =
                                    try {
                                        getFileNameFromUri(context, Uri.parse(iqamahSoundUri))
                                    } catch (e: Exception) {
                                        "Unknown"
                                    }
                                
                                // Check if URI is still accessible
                                val isAccessible = try {
                                    context.contentResolver.openFileDescriptor(Uri.parse(iqamahSoundUri), "r")?.close()
                                    true
                                } catch (e: Exception) {
                                    false
                                }
                                
                                if (isAccessible) {
                                    Text(
                                        text = stringResource(R.string.audio_file_selected, fileName),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                } else {
                                    Column(modifier = Modifier.padding(top = 4.dp)) {
                                        Text(
                                            text = "⚠️ File not accessible: $fileName",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                        Text(
                                            text = "Please re-select the audio file",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.no_audio_selected),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineDataSettings(
    zone: Int,
    viewModel: SettingsViewModel,
) {
    val prefetchState by viewModel.prefetchState.collectAsState()
    val cacheStatus by viewModel.cacheStatus.collectAsState()

    // Check cache status when this composable is first displayed
    LaunchedEffect(zone) {
        viewModel.checkCacheStatus(zone)
    }

    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(
                    text = "💾",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = "Offline Data",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = "Download prayer times for the remaining months to use offline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // Cache status
            cacheStatus?.let { status ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp),
                ) {
                    Icon(
                        imageVector = if (status.isFullyCached) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (status.isFullyCached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                    )
                    Text(
                        text = "${status.cachedMonths} of ${status.totalMonths} months cached (${status.cachedDays}/${status.totalDays} days)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (status.isFullyCached) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            )
                            Text(
                                text = "✓ All available months cached! App will work fully offline",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                } else if (status.cachedMonths > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            )
                            Text(
                                text = "Some months incomplete. Click download to update.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Download button
            Button(
                onClick = {
                    viewModel.prefetchOfflineData(zone)
                },
                enabled = prefetchState !is PrefetchState.Loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                when (prefetchState) {
                    is PrefetchState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Downloading...")
                    }
                    is PrefetchState.Success -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Downloaded Successfully")
                    }
                    is PrefetchState.Error -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry Download")
                    }
                    PrefetchState.Idle -> {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download for Offline Use")
                    }
                }
            }

            // Show result details
            when (val state = prefetchState) {
                is PrefetchState.Success -> {
                    if (state.result.successfulMonths > 0) {
                        Text(
                            text = "✓ Downloaded ${state.result.cachedDays} days from ${state.result.successfulMonths} new months",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (state.result.skippedMonths > 0) {
                        Text(
                            text = "⏭️ ${state.result.skippedMonths} months already cached (skipped)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (state.result.failedMonths > 0) {
                        Text(
                            text = "${state.result.failedMonths} months failed to download",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (state.result.unavailableMonths > 0) {
                        Text(
                            text = "${state.result.unavailableMonths} future months not available yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                is PrefetchState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                else -> { }
            }
        }
    }
}
