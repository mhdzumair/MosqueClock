package com.mosque.prayerclock.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.mosque.prayerclock.data.database.AppDatabase
import com.mosque.prayerclock.data.database.HijriDateDao
import com.mosque.prayerclock.data.database.PrayerTimesDao
import com.mosque.prayerclock.utils.DeviceUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Named

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Named("defaultScreenFaceId")
    fun provideDefaultScreenFaceId(@ApplicationContext context: Context): String =
        if (DeviceUtils.isTelevision(context)) "classic" else "mobile"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "mosque_clock_database",
            ).fallbackToDestructiveMigration() // Recreate DB on schema changes
            .build()

    @Provides
    fun providePrayerTimesDao(database: AppDatabase): PrayerTimesDao = database.prayerTimesDao()

    @Provides
    fun provideHijriDateDao(database: AppDatabase): HijriDateDao = database.hijriDateDao()

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.dataStore
}
