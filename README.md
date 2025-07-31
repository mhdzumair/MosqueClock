# Mosque Prayer Clock - Android TV Application

A comprehensive Android TV application designed for mosques to display prayer times, digital/analog clocks, and provide multilingual support (English/Tamil). The app fetches prayer times from the Sri Lankan Jamiath Ul Ulama server and provides a beautiful, customizable interface optimized for large screens.

## Features

### 🕌 Prayer Times Display
- **Real-time Prayer Times**: Fetches latest prayer times from Sri Lankan Jamiath Ul Ulama API
- **Five Daily Prayers**: Fajr, Dhuhr, Asr, Maghrib, Isha
- **Sunrise Time**: Additional sunrise information
- **Azan & Iqamah Times**: Shows both azan and iqamah times for each prayer
- **Next Prayer Highlight**: Automatically highlights the next upcoming prayer

### 🕐 Clock Features
- **Digital Clock**: Large, customizable digital time display
- **Analog Clock**: Beautiful analog clock with traditional design
- **Both Clocks**: Display both digital and analog clocks simultaneously
- **Time Formats**: Support for 12-hour and 24-hour formats
- **Seconds Display**: Option to show/hide seconds

### 🌍 Multilingual Support
- **English**: Full English language support
- **Tamil**: Complete Tamil language support (தமிழ்)
- **Dynamic Language Switching**: Change language in real-time

### ⚙️ Customization Options
- **Mosque Name**: Add custom mosque name display
- **Location Settings**: Configure region and city
- **Theme Selection**: Multiple themes including Default, Dark, Light, Mosque Green, Blue
- **Font Sizes**: Adjustable font sizes for better visibility
- **Clock Preferences**: Customize clock display options

### 📱 Android TV Optimized
- **TV Interface**: Designed specifically for Android TV and large screens
- **Remote Control Support**: Full D-pad and remote control navigation
- **Leanback Support**: Android TV Leanback launcher integration
- **Landscape Orientation**: Optimized for TV landscape display

## Technical Architecture

### Built With
- **Kotlin**: Modern Android development language
- **Jetpack Compose**: Modern UI toolkit for native Android
- **Android TV**: Optimized for TV devices
- **Hilt**: Dependency injection framework
- **Room Database**: Local data persistence
- **DataStore**: Settings and preferences storage
- **Retrofit**: REST API communication
- **Coroutines**: Asynchronous programming
- **Material Design 3**: Modern UI components

### Architecture Pattern
- **MVVM**: Model-View-ViewModel architecture
- **Repository Pattern**: Data layer abstraction
- **Clean Architecture**: Separation of concerns
- **Dependency Injection**: Hilt for dependency management

## Installation & Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 21 or higher
- Android TV device or emulator

### Build Instructions

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd MosqueClock
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the MosqueClock folder and select it

3. **Build the Project**
   ```bash
   ./gradlew build
   ```

4. **Install on Android TV**
   ```bash
   ./gradlew installDebug
   ```

### Running on Android TV Emulator

1. **Create Android TV AVD**
   - Open AVD Manager in Android Studio
   - Create a new Virtual Device
   - Select TV category
   - Choose Android TV (API 21+)
   - Configure and finish

2. **Run the Application**
   ```bash
   ./gradlew installDebug
   ```

## Configuration

### API Configuration
The app is configured to fetch prayer times from the Sri Lankan Jamiath Ul Ulama API. The base URL is:
```
https://api.srilankanjamiathululama.org/
```

### Default Settings
- **Language**: English
- **Location**: Colombo, Western Province
- **Clock Type**: Digital
- **Theme**: Default
- **Time Format**: 12-hour with seconds

## Usage

### Navigation
- **Menu Button**: Access settings screen
- **Back Button**: Return to main screen from settings
- **D-pad**: Navigate through settings options

### Main Screen
- **Prayer Times**: Displayed in horizontal scrollable cards
- **Current Time**: Large digital/analog clock display
- **Next Prayer**: Highlighted with different color
- **Mosque Name**: Displayed at the top (if configured)

### Settings Screen
- **Language**: Switch between English and Tamil
- **Location**: Configure region and city for prayer times
- **Mosque Name**: Set custom mosque name
- **Clock Type**: Choose between Digital, Analog, or Both
- **Theme**: Select from available themes
- **Time Format**: Configure 12/24 hour format and seconds display

## Data Storage

### Local Database (Room)
- **Prayer Times**: Cached locally for offline access
- **Auto-cleanup**: Old prayer times automatically removed after 30 days

### Settings (DataStore)
- **User Preferences**: Language, theme, location settings
- **Clock Configuration**: Display preferences
- **Mosque Information**: Custom mosque name and details

## API Integration

### Prayer Times API
The app integrates with the Sri Lankan Jamiath Ul Ulama prayer times API:

- **Today's Prayer Times**: `/prayer-times/today`
- **Monthly Prayer Times**: `/prayer-times`
- **Available Locations**: `/locations`

### Offline Support
- Prayer times are cached locally
- App works offline with cached data
- Automatic refresh when network is available

## Themes

### Available Themes
1. **Default**: System-based light/dark theme
2. **Light**: Light theme with green accents
3. **Dark**: Dark theme with green accents
4. **Mosque Green**: Green-themed design
5. **Blue**: Blue-themed design

## Troubleshooting

### Common Issues

1. **Prayer Times Not Loading**
   - Check internet connection
   - Verify API endpoint is accessible
   - Check location settings

2. **Theme Not Applying**
   - Restart the application
   - Check theme selection in settings

3. **Language Not Switching**
   - Ensure proper language selection
   - Restart application if needed

### Debug Mode
Enable debug logging by setting `HttpLoggingInterceptor.Level.BODY` in NetworkModule.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Sri Lankan Jamiath Ul Ulama for providing the prayer times API
- Android TV development community
- Jetpack Compose team for the modern UI toolkit

## Support

For support and questions, please create an issue in the repository or contact the development team.

---

**Note**: This application is designed specifically for mosque use and requires proper configuration of location settings to display accurate prayer times for your region.