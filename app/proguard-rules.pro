# Mosque Prayer Clock - ProGuard Rules
# Optimized for Android TV release builds

# Keep line numbers for debugging production crashes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*

# Optimization settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose
-allowaccessmodification
-repackageclasses ''

# Keep data classes for Gson serialization (already handled in Retrofit section)
# -keep class com.mosque.prayerclock.data.model.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class **_HiltComponents$* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# Keep Room database classes
-keep class androidx.room.** { *; }
-keep class com.mosque.prayerclock.data.database.** { *; }

# Retrofit - COMPREHENSIVE rules to preserve generic types
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes Exceptions

# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

# Keep Retrofit core classes
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# For Retrofit interface methods, keep everything to preserve generic signatures
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep Kotlin default parameter implementations
-keep class **$DefaultImpls { *; }

# Keep our API interfaces with generic signatures
-keep,allowobfuscation interface com.mosque.prayerclock.data.network.WeatherApi { *; }
-keep,allowobfuscation interface com.mosque.prayerclock.data.network.OpenWeatherMapApi { *; }
-keep,allowobfuscation interface com.mosque.prayerclock.data.network.MosqueClockApi { *; }
-keep,allowobfuscation interface com.mosque.prayerclock.data.network.PrayerTimesApi { *; }

# Keep all data classes in network package (API response models)
-keep class com.mosque.prayerclock.data.network.** { *; }

# Keep all data models used with Retrofit/Gson
-keep class com.mosque.prayerclock.data.model.** { <fields>; <methods>; }
-keepclassmembers class com.mosque.prayerclock.data.model.** { *; }

# Specifically keep weather response data classes with all fields
-keep class com.mosque.prayerclock.data.network.WeatherResponse { *; }
-keep class com.mosque.prayerclock.data.network.WeatherLocation { *; }
-keep class com.mosque.prayerclock.data.network.CurrentWeather { *; }
-keep class com.mosque.prayerclock.data.network.WeatherCondition { *; }
-keep class com.mosque.prayerclock.data.network.OpenWeatherMapResponse { *; }
-keep class com.mosque.prayerclock.data.network.OpenWeatherMapForecastResponse { *; }
-keep class com.mosque.prayerclock.data.network.Coordinates { *; }
-keep class com.mosque.prayerclock.data.network.WeatherDescription { *; }
-keep class com.mosque.prayerclock.data.network.MainWeatherData { *; }
-keep class com.mosque.prayerclock.data.network.WindData { *; }
-keep class com.mosque.prayerclock.data.network.CloudData { *; }
-keep class com.mosque.prayerclock.data.network.SystemData { *; }
-keep class com.mosque.prayerclock.data.network.ForecastItem { *; }
-keep class com.mosque.prayerclock.data.network.ForecastSystemData { *; }
-keep class com.mosque.prayerclock.data.network.CityInfo { *; }
-keep class com.mosque.prayerclock.data.network.ProcessedCurrentWeather { *; }

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# OkHttp Platform
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Kotlinx Serialization (not directly used but referenced by kotlinx-datetime)
-dontwarn kotlinx.serialization.**
-keep class kotlinx.serialization.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Kotlinx DateTime
-keep class kotlinx.datetime.** { *; }
-keep class kotlinx.datetime.serializers.** { *; }
-dontwarn kotlinx.datetime.serializers.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.**

# AndroidX
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**

# iTextPDF
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
-keepclassmembers class com.itextpdf.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }
-keepclassmembers class org.jsoup.** { *; }
-dontwarn org.jsoup.**
-dontwarn org.jspecify.annotations.**

# Remove all logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep BuildConfig
-keep class com.mosque.prayerclock.BuildConfig { *; }

# Additional missing class warnings suppression
-dontwarn org.jspecify.annotations.NullMarked
-dontwarn org.jspecify.annotations.Nullable
-dontwarn org.jspecify.annotations.NonNull

# Keep all enum classes (used by kotlinx-datetime)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}