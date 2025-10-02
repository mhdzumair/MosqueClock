import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.mosque.prayerclock"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mosque.prayerclock"
        minSdk = 21
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Generate universal APKs for all screen densities and ABIs
        vectorDrawables.useSupportLibrary = true

        // API keys and backend configuration are now managed via in-app settings
        // No build-time configuration required - configure everything in Settings screen!
        buildConfigField("String", "WEATHER_API_KEY", "\"\"")
        buildConfigField("String", "OPENWEATHERMAP_API_KEY", "\"\"")
        buildConfigField("String", "MOSQUE_CLOCK_API_URL", "\"http://10.0.2.2:8000/\"")
        buildConfigField("String", "MOSQUE_CLOCK_API_KEY", "\"\"")
    }

    signingConfigs {
        create("release") {
            // Load signing config from local.properties
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localProperties.load(localPropertiesFile.inputStream())
            }

            val keystorePath = localProperties.getProperty("RELEASE_STORE_FILE")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }

        release {
            // Use signing config if available, otherwise build will be unsigned
            signingConfig =
                if (rootProject.file("local.properties").exists() &&
                    Properties()
                        .apply { load(rootProject.file("local.properties").inputStream()) }
                        .getProperty("RELEASE_STORE_FILE") != null
                ) {
                    signingConfigs.getByName("release")
                } else {
                    null
                }

            // Re-enable minification with proper ProGuard rules
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = false
        warningsAsErrors = false
        xmlReport = true
        htmlReport = true
        disable +=
            setOf(
                "ObsoleteLintCustomCheck",
                "UnusedResources",
                "TypographyEllipsis",
                "CustomSplashScreen",
                "AppBundleLocaleChanges",
            )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    // Split configuration - Only generate universal APK
    splits {
        abi {
            isEnable = false // Disabled - only building universal APK
        }
        density {
            isEnable = false // Disable density splits - not needed for TV
        }
    }

    // Product flavors for different deployment scenarios (optional - comment out if not needed)
    // flavorDimensions += "deployment"
    // productFlavors {
    //     create("production") {
    //         dimension = "deployment"
    //         applicationIdSuffix = ""
    //         versionNameSuffix = ""
    //     }
    //     create("mosque") {
    //         dimension = "deployment"
    //         applicationIdSuffix = ".mosque"
    //         versionNameSuffix = "-mosque"
    //     }
    // }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.leanback:leanback:1.0.0")
    // implementation("androidx.tv:tv-foundation:1.0.0-alpha09")
    // implementation("androidx.tv:tv-material:1.0.0-alpha09")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation("androidx.compose.runtime:runtime-livedata")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // PDF Processing & Web Scraping
    implementation("com.itextpdf:itextg:5.5.10")
    implementation("org.jsoup:jsoup:1.17.2")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    kapt("com.google.dagger:hilt-compiler:2.48")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Preferences DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Date/Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}
