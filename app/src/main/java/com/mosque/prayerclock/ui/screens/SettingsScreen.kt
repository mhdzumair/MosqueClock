package com.mosque.prayerclock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.*
import com.mosque.prayerclock.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onExitApp: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            SettingsHeader(onNavigateBack = onNavigateBack)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    MosqueNameSetting(
                        mosqueName = settings.mosqueName,
                        onMosqueNameChange = viewModel::updateMosqueName
                    )
                }
                
                item {
                    LanguageSetting(
                        selectedLanguage = settings.language,
                        onLanguageChange = viewModel::updateLanguage
                    )
                }
                
                item {
                    LocationSettings(
                        city = settings.city,
                        country = settings.country,
                        onCityChange = viewModel::updateCity,
                        onCountryChange = viewModel::updateCountry
                    )
                }
                
                item {
                    WeatherCitySettings(
                        weatherCity = settings.weatherCity,
                        onWeatherCityChange = viewModel::updateWeatherCity
                    )
                }
                
                item {
                    ManualTimesToggle(
                        useManualTimes = settings.useManualTimes,
                        onToggle = viewModel::updateUseManualTimes
                    )
                }
                
                if (settings.useManualTimes) {
                    item {
                        ManualPrayerTimesSettings(
                            settings = settings,
                            onUpdateManualTime = viewModel::updateManualTime
                        )
                    }
                } else {
                    item {
                        IqamahGapSettings(
                            settings = settings,
                            onUpdateIqamahGap = viewModel::updateIqamahGap
                        )
                    }
                }
                
                item {
                    ClockTypeSetting(
                        selectedClockType = settings.clockType,
                        onClockTypeChange = viewModel::updateClockType
                    )
                }
                
                item {
                    ThemeSetting(
                        selectedTheme = settings.theme,
                        onThemeChange = viewModel::updateTheme
                    )
                }
                
                item {
                    ClockFormatSettings(
                        showSeconds = settings.showSeconds,
                        show24Hour = settings.show24HourFormat,
                        onShowSecondsChange = viewModel::updateShowSeconds,
                        onShow24HourChange = viewModel::updateShow24HourFormat
                    )
                }
                
                // Exit App button
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "App Control",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = onExitApp,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(
                                    text = "Exit Prayer Clock",
                                    color = MaterialTheme.colorScheme.onError,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onNavigateBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(stringResource(R.string.back))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MosqueNameSetting(
    mosqueName: String,
    onMosqueNameChange: (String) -> Unit
) {
    SettingsCard {
        Column {
            Text(
                text = stringResource(R.string.mosque_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ImprovedTextField(
                value = mosqueName,
                onValueChange = onMosqueNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Enter mosque name",
                keyboardType = KeyboardType.Text
            )
        }
    }
}

@Composable
private fun LanguageSetting(
    selectedLanguage: Language,
    onLanguageChange: (Language) -> Unit
) {
    SettingsCard {
        Column {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Language.values().forEach { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedLanguage == language,
                            onClick = { onLanguageChange(language) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedLanguage == language,
                        onClick = { onLanguageChange(language) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = language.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
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
    onCountryChange: (String) -> Unit
) {
    val cities = remember {
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
            "Perth" to "Australia"
        )
    }
    
    var showCityDropdown by remember { mutableStateOf(false) }
    
    SettingsCard {
        Column {
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // City Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showCityDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (city.isNotEmpty()) "$city, $country" else "Select City",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("▼", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                DropdownMenu(
                    expanded = showCityDropdown,
                    onDismissRequest = { showCityDropdown = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    cities.forEach { (cityName, countryName) ->
                        DropdownMenuItem(
                            text = {
                                Text("$cityName, $countryName")
                            },
                            onClick = {
                                onCityChange(cityName)
                                onCountryChange(countryName)
                                showCityDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherCitySettings(
    weatherCity: String,
    onWeatherCityChange: (String) -> Unit
) {
    SettingsCard {
        Column {
            Text(
                text = "Weather City",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "City for weather information (separate from prayer times location)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ImprovedTextField(
                value = weatherCity,
                onValueChange = onWeatherCityChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Enter city name for weather",
                keyboardType = KeyboardType.Text
            )
        }
    }
}

@Composable
private fun ManualTimesToggle(
    useManualTimes: Boolean,
    onToggle: (Boolean) -> Unit
) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Manual Prayer Times",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Use manual times instead of API",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Switch(
                checked = useManualTimes,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun ManualPrayerTimesSettings(
    settings: AppSettings,
    onUpdateManualTime: (String, String) -> Unit
) {
    SettingsCard {
        Column {
            Text(
                text = "Prayer Times Setup",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PrayerTimeInput(
                label = "Fajr Azan",
                time = settings.manualFajrAzan,
                onTimeChange = { onUpdateManualTime("fajrAzan", it) }
            )
            
            PrayerTimeInput(
                label = "Fajr Iqamah",
                time = settings.manualFajrIqamah,
                onTimeChange = { onUpdateManualTime("fajrIqamah", it) }
            )
            
            PrayerTimeInput(
                label = "Dhuhr Azan",
                time = settings.manualDhuhrAzan,
                onTimeChange = { onUpdateManualTime("dhuhrAzan", it) }
            )
            
            PrayerTimeInput(
                label = "Dhuhr Iqamah",
                time = settings.manualDhuhrIqamah,
                onTimeChange = { onUpdateManualTime("dhuhrIqamah", it) }
            )
            
            PrayerTimeInput(
                label = "Asr Azan",
                time = settings.manualAsrAzan,
                onTimeChange = { onUpdateManualTime("asrAzan", it) }
            )
            
            PrayerTimeInput(
                label = "Asr Iqamah",
                time = settings.manualAsrIqamah,
                onTimeChange = { onUpdateManualTime("asrIqamah", it) }
            )
            
            PrayerTimeInput(
                label = "Maghrib Azan",
                time = settings.manualMaghribAzan,
                onTimeChange = { onUpdateManualTime("maghribAzan", it) }
            )
            
            PrayerTimeInput(
                label = "Maghrib Iqamah",
                time = settings.manualMaghribIqamah,
                onTimeChange = { onUpdateManualTime("maghribIqamah", it) }
            )
            
            PrayerTimeInput(
                label = "Isha Azan",
                time = settings.manualIshaAzan,
                onTimeChange = { onUpdateManualTime("ishaAzan", it) }
            )
            
            PrayerTimeInput(
                label = "Isha Iqamah",
                time = settings.manualIshaIqamah,
                onTimeChange = { onUpdateManualTime("ishaIqamah", it) }
            )
        }
    }
}

@Composable
private fun IqamahGapSettings(
    settings: AppSettings,
    onUpdateIqamahGap: (String, Int) -> Unit
) {
    SettingsCard {
        Column {
            Text(
                text = "Iqamah Time Gaps (minutes after Azan)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            IqamahGapInput(
                label = "Fajr",
                gap = settings.fajrIqamahGap,
                onGapChange = { onUpdateIqamahGap("fajr", it) }
            )
            
            IqamahGapInput(
                label = "Dhuhr",
                gap = settings.dhuhrIqamahGap,
                onGapChange = { onUpdateIqamahGap("dhuhr", it) }
            )
            
            IqamahGapInput(
                label = "Asr",
                gap = settings.asrIqamahGap,
                onGapChange = { onUpdateIqamahGap("asr", it) }
            )
            
            IqamahGapInput(
                label = "Maghrib",
                gap = settings.maghribIqamahGap,
                onGapChange = { onUpdateIqamahGap("maghrib", it) }
            )
            
            IqamahGapInput(
                label = "Isha",
                gap = settings.ishaIqamahGap,
                onGapChange = { onUpdateIqamahGap("isha", it) }
            )
        }
    }
}

@Composable
private fun PrayerTimeInput(
    label: String,
    time: String,
    onTimeChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        TimePickerField(
            value = time,
            onValueChange = onTimeChange,
            modifier = Modifier.width(120.dp)
        )
    }
}

@Composable
private fun IqamahGapInput(
    label: String,
    gap: Int,
    onGapChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        NumberPickerField(
            value = gap,
            onValueChange = onGapChange,
            modifier = Modifier.width(80.dp),
            range = 0..60
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "min",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ClockTypeSetting(
    selectedClockType: ClockType,
    onClockTypeChange: (ClockType) -> Unit
) {
    SettingsCard {
        Column {
            Text(
                text = stringResource(R.string.clock_type),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ClockType.values().forEach { clockType ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedClockType == clockType,
                            onClick = { onClockTypeChange(clockType) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedClockType == clockType,
                        onClick = { onClockTypeChange(clockType) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = when (clockType) {
                            ClockType.DIGITAL -> stringResource(R.string.digital_clock)
                            ClockType.ANALOG -> stringResource(R.string.analog_clock)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSetting(
    selectedTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    SettingsCard {
        Column {
            Text(
                text = stringResource(R.string.theme),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppTheme.values().forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedTheme == theme,
                            onClick = { onThemeChange(theme) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedTheme == theme,
                        onClick = { onThemeChange(theme) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = when (theme) {
                            AppTheme.DEFAULT -> stringResource(R.string.theme_default)
                            AppTheme.DARK -> stringResource(R.string.theme_dark)
                            AppTheme.LIGHT -> stringResource(R.string.theme_light)
                            AppTheme.MOSQUE_GREEN -> stringResource(R.string.theme_mosque_green)
                            AppTheme.BLUE -> stringResource(R.string.theme_blue)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ClockFormatSettings(
    showSeconds: Boolean,
    show24Hour: Boolean,
    onShowSecondsChange: (Boolean) -> Unit,
    onShow24HourChange: (Boolean) -> Unit
) {
    SettingsCard {
        Column {
            Text(
                text = "Clock Format",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.show_seconds),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Switch(
                    checked = showSeconds,
                    onCheckedChange = onShowSecondsChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.hour_format_24),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Switch(
                    checked = show24Hour,
                    onCheckedChange = onShow24HourChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.padding(16.dp)
        ) {
                content()
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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val focusManager = LocalFocusManager.current
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .fillMaxWidth(),
        label = label?.let { { Text(it) } },
        placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            },
            onDone = {
                focusManager.clearFocus()
            }
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun TimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    
    // Parse initial value
    LaunchedEffect(value) {
        val parts = value.split(":")
        if (parts.size == 2) {
            hours = parts[0].padStart(2, '0')
            minutes = parts[1].padStart(2, '0')
        }
    }
    
    // Update value when hours or minutes change
    LaunchedEffect(hours, minutes) {
        if (hours.isNotEmpty() && minutes.isNotEmpty()) {
            val h = hours.toIntOrNull()?.coerceIn(0, 23) ?: 0
            val m = minutes.toIntOrNull()?.coerceIn(0, 59) ?: 0
            onValueChange(String.format("%02d:%02d", h, m))
        }
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hours picker
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        val currentHour = hours.toIntOrNull() ?: 0
                        val newHour = (currentHour + 1) % 24
                        hours = String.format("%02d", newHour)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = "▲",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = hours.padStart(2, '0'),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                
                IconButton(
                    onClick = {
                        val currentHour = hours.toIntOrNull() ?: 0
                        val newHour = if (currentHour == 0) 23 else currentHour - 1
                        hours = String.format("%02d", newHour)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = "▼",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Text(
                    text = "HH",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            // Minutes picker
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        val currentMin = minutes.toIntOrNull() ?: 0
                        val newMin = (currentMin + 1) % 60
                        minutes = String.format("%02d", newMin)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = "▲",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = minutes.padStart(2, '0'),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                
                IconButton(
                    onClick = {
                        val currentMin = minutes.toIntOrNull() ?: 0
                        val newMin = if (currentMin == 0) 59 else currentMin - 1
                        minutes = String.format("%02d", newMin)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = "▼",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Text(
                    text = "MM",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
    range: IntRange = 0..99
) {
    val focusManager = LocalFocusManager.current
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decrease button
        IconButton(
            onClick = {
                val newValue = (value - 1).coerceIn(range)
                onValueChange(newValue)
            },
            enabled = value > range.first
        ) {
            Text(
                text = "-",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Number display/input
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { intValue ->
                    if (intValue in range) {
                        onValueChange(intValue)
                    }
                }
            },
            modifier = Modifier.width(60.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        )
        
        // Increase button
        IconButton(
            onClick = {
                val newValue = (value + 1).coerceIn(range)
                onValueChange(newValue)
            },
            enabled = value < range.last
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}