# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android TV application for mosques that displays prayer times, digital/analog clocks, and provides multilingual support (English/Tamil). The app fetches prayer times from the AlAdhan API and provides a beautiful, customizable interface optimized for large screens.

## Build and Development Commands

### Building the Project
```bash
./gradlew build                 # Build the entire project
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
```

### Installation and Running
```bash
./gradlew installDebug         # Install debug build on connected device/emulator
./gradlew uninstallDebug       # Uninstall debug build
```

### Testing
```bash
./gradlew test                 # Run unit tests
./gradlew connectedAndroidTest # Run instrumented tests
./gradlew testDebugUnitTest    # Run debug unit tests specifically
```

### Code Quality
```bash
./gradlew lint                 # Run Android lint checks
./gradlew lintDebug           # Run lint for debug build
```

### Cleaning
```bash
./gradlew clean               # Clean build artifacts
```

## Architecture Overview

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt/Dagger
- **Database**: Room (local caching)
- **Settings**: DataStore Preferences
- **Network**: Retrofit with OkHttp
- **Async**: Kotlin Coroutines and Flow
- **Date/Time**: kotlinx-datetime

### Key Architecture Components

#### Data Layer
- **Repository Pattern**: `PrayerTimesRepository` and `SettingsRepository` abstract data sources
- **Network Layer**: `PrayerTimesApi` interfaces with AlAdhan API (https://api.aladhan.com/)
- **Local Storage**: Room database caches prayer times for offline access
- **Settings**: DataStore handles user preferences and configuration

#### Domain Models
- `PrayerTimes`: Core entity containing prayer time data with both Azan and Iqamah times
- `AppSettings`: Configuration model including location, themes, clock preferences
- `NetworkResult<T>`: Sealed class wrapping API responses (Loading/Success/Error states)

#### UI Layer
- **MVVM**: ViewModels (`MainViewModel`, `SettingsViewModel`) manage UI state
- **State Management**: StateFlow and LiveData for reactive UI updates
- **Compose UI**: Modern declarative UI with custom components (`DigitalClock`, `AnalogClock`, `PrayerTimeCard`)

#### Dependency Injection Structure
- `DatabaseModule`: Provides Room database and DataStore instances
- `NetworkModule`: Configures Retrofit, OkHttp with logging interceptor
- All repositories and ViewModels are @Singleton scoped through Hilt

### Key Data Flow
1. ViewModels request data from Repositories
2. Repositories check local cache (Room) first for offline support
3. If cache miss or expired, fetch from AlAdhan API
4. API responses are transformed to domain models and cached locally
5. UI state flows reactively update Compose components
6. Old prayer times are automatically cleaned up after 30 days

### Navigation and Screens
- `MainScreen`: Primary display with clocks and prayer time cards
- `SettingsScreen`: Configuration for location, themes, manual times, clock preferences
- Navigation uses Compose Navigation with Hilt integration

### Prayer Time Logic
- Supports both automatic API fetching and manual time entry
- Iqamah times calculated with configurable gaps after Azan times
- Next prayer highlighting based on current time comparison
- Multilingual display names (English/Tamil) for prayers

### Android TV Specific Features
- Leanback launcher integration via AndroidManifest
- D-pad navigation optimized for TV remotes
- Large screen layouts and font sizes
- Landscape orientation focus
- TV-specific UI components and navigation patterns

## Important Notes

### API Configuration
The app currently uses AlAdhan API (https://api.aladhan.com/). The actual implementation in `NetworkModule.kt:38` uses AlAdhan.

### Debugging
Enable detailed network logging by ensuring `HttpLoggingInterceptor.Level.BODY` is set in `NetworkModule.kt` (currently enabled).

### Themes and Localization
- Supports 5 themes: Default, Light, Dark, Mosque Green, Blue
- Tamil language support with proper Unicode rendering
- Theme switching affects entire app UI dynamically

### Database Schema
- Room database named "mosque_clock_database"
- Automatic cleanup of prayer times older than 30 days
- Supports both online/offline modes seamlessly