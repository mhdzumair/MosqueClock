package com.mosque.prayerclock.di

import com.mosque.prayerclock.data.network.PrayerTimesApi
import com.mosque.prayerclock.data.network.WeatherApi
import com.mosque.prayerclock.data.network.MosqueClockApi
import com.mosque.prayerclock.data.network.ApiKeyInterceptor
import com.mosque.prayerclock.data.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    @Named("mosque_clock")
    fun provideMosqueClockOkHttpClient(apiKeyInterceptor: ApiKeyInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(apiKeyInterceptor) // Add API key interceptor for authentication
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    @Named("prayer")
    fun providePrayerRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    @Named("weather")
    fun provideWeatherRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.weatherapi.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    @Named("mosque_clock")
    fun provideMosqueClockRetrofit(@Named("mosque_clock") okHttpClient: OkHttpClient): Retrofit {
        // Use 10.0.2.2 for Android emulator to access host machine's localhost
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun providePrayerTimesApi(@Named("prayer") retrofit: Retrofit): PrayerTimesApi {
        return retrofit.create(PrayerTimesApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideWeatherApi(@Named("weather") retrofit: Retrofit): WeatherApi {
        return retrofit.create(WeatherApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideMosqueClockApi(@Named("mosque_clock") retrofit: Retrofit): MosqueClockApi {
        return retrofit.create(MosqueClockApi::class.java)
    }
}