package com.mosque.prayerclock.di

import com.mosque.prayerclock.BuildConfig
import com.mosque.prayerclock.data.network.ApiKeyInterceptor
import com.mosque.prayerclock.data.network.MosqueClockApi
import com.mosque.prayerclock.data.network.OpenWeatherMapApi
import com.mosque.prayerclock.data.network.PrayerTimesApi
import com.mosque.prayerclock.data.network.WeatherApi
import com.mosque.prayerclock.data.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                // NONE = no logging, BASIC = request/response line only
                // HEADERS = request/response line + headers, BODY = everything
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC // Less verbose for debug builds
                } else {
                    HttpLoggingInterceptor.Level.NONE // No logging in release
                }
            }

        return OkHttpClient
            .Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS) // Reduced for limited connections
            .readTimeout(15, TimeUnit.SECONDS) // Reduced for limited connections
            .writeTimeout(10, TimeUnit.SECONDS) // Reduced for limited connections
            .retryOnConnectionFailure(true) // Enable retry on connection failure
            .build()
    }

    @Provides
    @Singleton
    @Named("mosque_clock")
    fun provideMosqueClockOkHttpClient(apiKeyInterceptor: ApiKeyInterceptor): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                // NONE = no logging, BASIC = request/response line only
                // HEADERS = request/response line + headers, BODY = everything
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC // Less verbose for debug builds
                } else {
                    HttpLoggingInterceptor.Level.NONE // No logging in release
                }
            }

        return OkHttpClient
            .Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(apiKeyInterceptor) // Add API key interceptor for authentication
            .connectTimeout(10, TimeUnit.SECONDS) // Reduced for limited connections
            .readTimeout(15, TimeUnit.SECONDS) // Reduced for limited connections
            .writeTimeout(10, TimeUnit.SECONDS) // Reduced for limited connections
            .retryOnConnectionFailure(true) // Enable retry on connection failure
            .build()
    }

    @Provides
    @Singleton
    @Named("prayer")
    fun providePrayerRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit
            .Builder()
            .baseUrl("https://api.aladhan.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @Named("weather")
    fun provideWeatherRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit
            .Builder()
            .baseUrl("https://api.weatherapi.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @Named("mosque_clock")
    fun provideMosqueClockRetrofit(
        @Named("mosque_clock") okHttpClient: OkHttpClient,
    ): Retrofit {
        // Use configurable API URL from local.properties
        return Retrofit
            .Builder()
            .baseUrl(BuildConfig.MOSQUE_CLOCK_API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("openweathermap")
    fun provideOpenWeatherMapRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit
            .Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun providePrayerTimesApi(
        @Named("prayer") retrofit: Retrofit,
    ): PrayerTimesApi = retrofit.create(PrayerTimesApi::class.java)

    @Provides
    @Singleton
    fun provideWeatherApi(
        @Named("weather") retrofit: Retrofit,
    ): WeatherApi = retrofit.create(WeatherApi::class.java)

    @Provides
    @Singleton
    fun provideMosqueClockApi(
        @Named("mosque_clock") retrofit: Retrofit,
    ): MosqueClockApi = retrofit.create(MosqueClockApi::class.java)

    @Provides
    @Singleton
    fun provideOpenWeatherMapApi(
        @Named("openweathermap") retrofit: Retrofit,
    ): OpenWeatherMapApi = retrofit.create(OpenWeatherMapApi::class.java)
}
