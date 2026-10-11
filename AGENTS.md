# AGENTS.md

This file provides guidance to coding agents working in this repository.

Use the built-in file editing tools for file changes. Use the CLI for command execution, builds, emulator control, and preview capture.

## Project Overview

This is an Android TV application for mosques that displays prayer times, digital/analog clocks, and provides multilingual support (English/Tamil/Sinhala). The app fetches prayer times from the AlAdhan API and provides a beautiful, customizable interface optimized for large screens.

## Build and Development Commands

### Building the Project
```bash
./gradlew build                 # Build the entire project
./gradlew :app:assemblePlaystoreDebug # Build preview-compatible debug APK
./gradlew :app:assemblePlaystoreRelease # Build Play Store release APK
```

The app has `playstore` and `github` product flavors. Use explicit variant task names; tasks such as `compileDebugKotlin` are ambiguous. For GitHub builds, substitute `Github` for `Playstore`. The face preview script uses `PlaystoreDebug`.

### Installation and Running
```bash
./gradlew :app:installPlaystoreDebug # Install debug build on connected device/emulator
./gradlew :app:uninstallPlaystoreDebug # Uninstall debug build
```

### Testing
```bash
./gradlew test                 # Run unit tests
./gradlew connectedAndroidTest # Run instrumented tests
./gradlew :app:testPlaystoreDebugUnitTest # Run Play Store debug unit tests
```

### Code Quality
```bash
./gradlew lint                 # Run Android lint checks
./gradlew :app:lintPlaystoreDebug # Run lint for Play Store debug build
```

### Cleaning
```bash
./gradlew clean               # Clean build artifacts
```

### Required Clock Face Verification

For clock face changes involving layout, spacing, typography, colors, or rendering, run the app on the Android TV emulator using `scripts/capture_face_scenarios.sh`. Compilation alone is insufficient verification.

1. Use `adb devices -l` to confirm that an Android TV emulator is connected. If multiple devices are connected, set `ANDROID_SERIAL` to the TV emulator serial for the script and other adb commands.
2. Capture the affected face in English, Tamil, and Sinhala at 1920x1080 and 1280x720. Include every affected scenario. For dashboard or timetable changes, include `digital`, `analog`, `azan-hour`, `iqamah-hour`, and `after-isha`.
3. Open and visually inspect the captured screenshots. Check text clipping, overlapping labels, column alignment, prayer row spacing, next prayer highlighting, and use of available space. Inspect localized names and both clock modes.
4. Fix any issues and repeat the affected captures. Report the emulator, scenarios, languages, resolutions, and screenshot directory in the final response. If the emulator cannot be used, report that verification is incomplete and explain the blocker.

Example for Noor dashboard changes:

```bash
FACE_PREVIEW_FACES=noor \
FACE_PREVIEW_SCENARIOS=digital,analog,azan-hour,iqamah-hour,after-isha \
FACE_PREVIEW_LANGUAGES=en,ta,si \
FACE_PREVIEW_RESOLUTIONS=1920x1080,1280x720 \
./scripts/capture_face_scenarios.sh build/noor-verification
```

The script builds and installs `:app:installPlaystoreDebug`, launches `FacePreviewActivity` with deterministic preview data, verifies that each scene renders, checks that the app remains running, saves PNG screenshots, and restores the original display size. Keep UI verification enabled (the default). Use a dedicated output directory because the script cleans that directory by default. Use `FACE_PREVIEW_INSTALL=false` only when the current changes are already installed. Also capture `FACE_PREVIEW_SHOW_24_HOUR=true` or `FACE_PREVIEW_SHOW_SECONDS=false` when those settings affect the change.

If debug installation fails with `INSTALL_FAILED_VERSION_DOWNGRADE`, build `:app:assemblePlaystoreDebug`, install that APK using `adb install -r -d app/build/outputs/apk/playstore/debug/app-playstore-debug.apk`, and rerun the preview with `FACE_PREVIEW_INSTALL=false`. This permits a debug downgrade while retaining emulator app data.

Use `scripts/generate_face_gallery.sh` when updating the documentation gallery. Gallery generation is supplementary; the raw screenshots from the verification script should be inspected at full resolution.

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
