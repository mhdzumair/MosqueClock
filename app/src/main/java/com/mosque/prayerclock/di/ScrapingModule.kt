package com.mosque.prayerclock.di

import com.google.gson.Gson
import com.mosque.prayerclock.data.database.PrayerTimesDao
import com.mosque.prayerclock.data.scraping.ACJUScraper
import com.mosque.prayerclock.data.scraping.DirectScrapingService
import com.mosque.prayerclock.data.scraping.HijriDateScraper
import com.mosque.prayerclock.data.scraping.PdfParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScrapingModule {

    @Provides
    @Singleton
    fun providePdfParser(): PdfParser {
        return PdfParser()
    }

    @Provides
    @Singleton
    fun provideACJUScraper(okHttpClient: OkHttpClient): ACJUScraper {
        return ACJUScraper(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideHijriDateScraper(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): HijriDateScraper {
        return HijriDateScraper(okHttpClient, gson)
    }

    @Provides
    @Singleton
    fun provideDirectScrapingService(
        acjuScraper: ACJUScraper,
        pdfParser: PdfParser,
        hijriDateScraper: HijriDateScraper,
        prayerTimesDao: PrayerTimesDao
    ): DirectScrapingService {
        return DirectScrapingService(acjuScraper, pdfParser, hijriDateScraper, prayerTimesDao)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}

