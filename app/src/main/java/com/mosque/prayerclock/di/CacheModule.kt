package com.mosque.prayerclock.di

import com.mosque.prayerclock.data.cache.PrayerTimesCacheInvalidator
import com.mosque.prayerclock.data.repository.PrayerTimesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CacheModule {

    @Binds
    @Singleton
    abstract fun bindPrayerTimesCacheInvalidator(
        prayerTimesRepository: PrayerTimesRepository
    ): PrayerTimesCacheInvalidator
}

